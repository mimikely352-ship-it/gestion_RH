package com.sirh.presence.service;

import com.sirh.Application;
import com.sirh.commun.enums.Role;
import com.sirh.config.JwtService;
import com.sirh.embauche.entity.DossierEmploye;
import com.sirh.embauche.repository.DossierEmployeRepository;
import com.sirh.organisation.entity.*;
import com.sirh.organisation.repository.*;
import com.sirh.presence.entity.*;
import com.sirh.utilisateur.entity.Utilisateur;
import com.sirh.utilisateur.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.net.URI;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in PostgreSQL test. All tables and synthetic records live in a disposable schema. */
@EnabledIfEnvironmentVariable(named="SIRH_PRESENCE_INTEGRATION",matches="true")
class PresenceIntegrationTest {
    @Test void migrationJpaParcoursCongePointageEtVuesHttp() throws Exception {
        var env=System.getenv();
        String url=env.getOrDefault("SIRH_TEST_DB_URL","jdbc:postgresql://localhost:5432/sirh");
        String user=env.getOrDefault("SIRH_TEST_DB_USER","postgres");
        String password=env.getOrDefault("SIRH_TEST_DB_PASSWORD","");
        String schema="codex_presence_"+UUID.randomUUID().toString().replace("-","");
        if(!schema.matches("codex_presence_[a-f0-9]{32}")) throw new IllegalStateException("Invalid test schema");
        ConfigurableApplicationContext context=null;
        boolean cree=false;
        String piece=null;
        com.sirh.candidat.service.FileStorageService stockage=null;
        try {
            try(var c=DriverManager.getConnection(url,user,password)) {
                c.createStatement().execute("CREATE SCHEMA "+schema); cree=true; c.setSchema(schema); c.setAutoCommit(false);
                for(String fichier:List.of("sql/01_schema.sql","sql/11_entreprises_offres.sql")) {
                    String sql=Files.readString(Path.of(fichier),StandardCharsets.UTF_8)
                            .replaceAll("(?m)^\\\\.*$","").replaceAll("(?mi)^\\s*(BEGIN|COMMIT);\\s*$","");
                    ScriptUtils.executeSqlScript(c,new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
                }
                c.commit();
            }
            context=new SpringApplicationBuilder(Application.class).run(
                    "--server.port=0","--app.demo-data.enabled=false","--app.schema-migrations.enabled=true",
                    "--spring.datasource.url="+url+(url.contains("?")?"&":"?")+"currentSchema="+schema,
                    "--spring.datasource.username="+user,"--spring.datasource.password="+password,
                    "--app.upload.dir=build-bloc7/integration-uploads","--app.jwt.cookie-secure=false",
                    "--app.conges.validation-manager=true","--app.conges.jours-calendaires=false",
                    "--logging.level.root=ERROR","--logging.level.com.sirh=ERROR","--spring.main.banner-mode=off");
            var utilisateurs=context.getBean(UtilisateurRepository.class);
            var direction=context.getBean(DirectionRepository.class).save(Direction.builder().nom("Direction test").build());
            var departement=context.getBean(DepartementRepository.class).save(Departement.builder().nom("Département test").direction(direction).build());
            var chef=utilisateurs.save(Utilisateur.builder().email("chef-presence@test.local").motDePasse("unused-test").role(Role.CHEF_SERVICE).actif(true).build());
            var service=context.getBean(ServiceRepository.class).save(com.sirh.organisation.entity.Service.builder().nom("Service test").departement(departement).chefService(chef).build());
            chef.setService(service); utilisateurs.save(chef);
            var employe=utilisateurs.save(Utilisateur.builder().email("emp-presence@test.local").motDePasse("unused-test").role(Role.EMPLOYE).service(service).actif(true).build());
            var rh=utilisateurs.save(Utilisateur.builder().email("rh-presence@test.local").motDePasse("unused-test").role(Role.RH).actif(true).build());
            var autre=utilisateurs.save(Utilisateur.builder().email("autre-presence@test.local").motDePasse("unused-test").role(Role.EMPLOYE).actif(true).build());
            var clock=context.getBean(Clock.class); var aujourdHui=LocalDate.now(clock);
            var dossier=context.getBean(DossierEmployeRepository.class).save(DossierEmploye.builder().utilisateur(employe).nom("Test").prenom("Employé").dateEntree(aujourdHui.minusDays(40)).build());
            Authentication authEmp=new UsernamePasswordAuthenticationToken(employe.getEmail(),"");
            Authentication authChef=new UsernamePasswordAuthenticationToken(chef.getEmail(),"");
            Authentication authRh=new UsernamePasswordAuthenticationToken(rh.getEmail(),"");
            var conges=context.getBean(CongeService.class); var temps=context.getBean(TempsService.class);
            var type=conges.types().stream().filter(TypeConge::isDeduitSolde).findFirst().orElseThrow();
            var debut=aujourdHui.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
            if(debut.plusDays(4).getYear()!=debut.getYear()) debut=debut.plusWeeks(1);
            var fin=debut.plusDays(4);
            conges.definirDroit(dossier.getId(),type.getId(),debut.getYear(),10,"Droits test",authRh);
            conges.soumettre(type.getId(),debut,fin,"Congé test",null,authEmp);
            var demande=conges.historique(dossier.getId(),authEmp).get(0);
            assertEquals(5,demande.getJours());
            conges.decider(demande.getId(),true,null,authChef);
            conges.decider(demande.getId(),true,null,authRh);
            assertEquals(5,conges.soldes(dossier.getId(),debut.getYear(),authEmp).stream().filter(s -> s.getType().getId().equals(type.getId())).findFirst().orElseThrow().getDisponible());
            assertTrue(temps.calendrier(dossier.getId(),debut,fin,authEmp).stream().allMatch(j -> !j.getAbsence().isEmpty()));
            conges.annuler(demande.getId(),"Annulation test",authRh);
            assertEquals(10,conges.soldes(dossier.getId(),debut.getYear(),authEmp).stream().filter(s -> s.getType().getId().equals(type.getId())).findFirst().orElseThrow().getDisponible());
            var jourPointe=aujourdHui.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
            temps.corriger(dossier.getId(),jourPointe,LocalTime.of(9,0),LocalTime.of(19,0),60,60,"Correction test",authRh);
            assertEquals(540,temps.calendrier(dossier.getId(),jourPointe,jourPointe,authRh).get(0).getTravaillees());
            var pdf=new MockMultipartFile("justificatif","justificatif.pdf","application/pdf","%PDF-1.4\nTest\n".getBytes(StandardCharsets.US_ASCII));
            conges.absence(dossier.getId(),debut.plusWeeks(2),debut.plusWeeks(2),"Absence test",false,pdf,authRh);
            var absence=conges.absences(dossier.getId(),authRh).stream().filter(a -> a.getJustificatifChemin()!=null).findFirst().orElseThrow();
            piece=absence.getJustificatifChemin(); stockage=context.getBean(com.sirh.candidat.service.FileStorageService.class);
            var jwt=context.getBean(JwtService.class);
            int port=((ServletWebServerApplicationContext)context).getWebServer().getPort();
            for(String page:List.of("/presence","/conges")) assertEquals(200,get(port,page,jwt,employe).statusCode(),page);
            assertEquals(200,get(port,"/conges/validations",jwt,chef).statusCode());
            assertEquals(200,get(port,"/rh/presence?dossierId="+dossier.getId(),jwt,rh).statusCode());
            assertEquals(200,get(port,"/rh/conges?dossierId="+dossier.getId(),jwt,rh).statusCode());
            String export="/rh/presence/variables.csv?dossierId="+dossier.getId()+"&debut="+jourPointe+"&fin="+jourPointe;
            var csv=get(port,export,jwt,rh); assertEquals(200,csv.statusCode()); assertTrue(csv.body().contains(";540;0;60;60;"));
            assertEquals(403,get(port,export,jwt,employe).statusCode());
            String fichier="/absences/"+absence.getId()+"/justificatif";
            assertEquals(200,get(port,fichier,jwt,employe).statusCode());
            assertEquals(403,get(port,fichier,jwt,chef).statusCode());
            assertEquals(403,get(port,fichier,jwt,autre).statusCode());
            // Two non-overlapping requests cannot spend the same three-day balance.
            conges.definirDroit(dossier.getId(),type.getId(),debut.getYear(),3,"Concurrence test",authRh);
            final LocalDate lundi=debut;
            var depart=new java.util.concurrent.CountDownLatch(1);
            var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                var futures=new ArrayList<java.util.concurrent.Future<Boolean>>();
                for(int index=0;index<2;index++) {
                    final int decalage=index*2;
                    futures.add(pool.submit(() -> {
                        depart.await();
                        try { conges.soumettre(type.getId(),lundi.plusDays(decalage),lundi.plusDays(decalage+1),"Concurrence",null,authEmp);return true; }
                        catch(com.sirh.commun.exception.BusinessException soldeInsuffisant) { return false; }
                    }));
                }
                depart.countDown(); int succes=0;
                for(var future:futures) if(future.get(20,java.util.concurrent.TimeUnit.SECONDS)) succes++;
                assertEquals(1,succes);
                assertEquals(1,conges.soldes(dossier.getId(),debut.getYear(),authEmp).stream().filter(s -> s.getType().getId().equals(type.getId())).findFirst().orElseThrow().getDisponible());
            } finally { pool.shutdownNow(); }
        } finally {
            if(stockage!=null && piece!=null) stockage.supprimer(piece);
            if(context!=null) context.close();
            if(cree) try(var c=DriverManager.getConnection(url,user,password)) { c.createStatement().execute("DROP SCHEMA "+schema+" CASCADE"); }
        }
    }
    private HttpResponse<String> get(int port,String path,JwtService jwt,Utilisateur u) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).timeout(Duration.ofSeconds(30))
                .header("Authorization","Bearer "+jwt.generer(u.getId(),u.getEmail(),u.getRole().name())).GET().build();
        return HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
    }
}
