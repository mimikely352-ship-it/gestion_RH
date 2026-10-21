	Page recrutement avec retour fictif avec login email + password

# Type Utilisateur:
- Utilisateur -> Acceptation, sns
- Candidat -> Form

## Candidat pas encore pris:
	### Remplir formulaire de candidature
	-- Nom
	-- Prenom
	-- Email
	-- Numero tel
	-- Date et lieu de naissance
	-- Num CIN
	-- Import pdf CV

	### Test
		Seule les candidat prisent peuvent faire les tests avec 3 niveaux

	### Dernier entretient:
	- Salaire, date/heure, conge par mois, avantage


## User_type:
- DG: SuperAdmin
- RH: Acceptation(Candidature, requet, Demande), Demande -> Direction, exportation, Visuel(Salaire, candidat, Resulta)
- Manager: Demande(recrutement, *meme qu'employer), Acceptation demande employer
- Employer: Demande(Augmentation de paye, promotion, \congee, Changement de poste, demission, plainte)

### Login:
- matricule
- password


# User_employer:
- Direction -> Departement -> Service -> Section

## Role:
- Super_admin: >= DG
- Admin: RH
- Manager(niveau 1 -> 4): Directeur, Chef de departement, Chef de service. chef de section
- Utilisateur: Employer


