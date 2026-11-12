package com.sirh.presence.service;
import com.sirh.presence.entity.TypeConge;
import lombok.Value;
@Value
public class SoldeConge {
    TypeConge type; int annee; int droits; int reserves; int consommes;
    public int getDisponible() { return droits-reserves-consommes; }
}
