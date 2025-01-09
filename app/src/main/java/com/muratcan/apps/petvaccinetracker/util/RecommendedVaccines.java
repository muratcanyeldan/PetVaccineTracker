package com.muratcan.apps.petvaccinetracker.util;

import com.muratcan.apps.petvaccinetracker.model.Vaccine;

import java.util.ArrayList;
import java.util.List;

public class RecommendedVaccines {
    
    public static List<Vaccine> getRecommendedVaccines(String petType, long petId) {
        List<Vaccine> vaccines = new ArrayList<>();
        
        if (petType.equalsIgnoreCase("Dog") || petType.equalsIgnoreCase("Köpek")) {
            // Core vaccines for dogs
            addVaccine(vaccines, "Rabies", "Core vaccine for dogs", petId);
            addVaccine(vaccines, "Distemper (DHPP)", "Core vaccine protecting against Distemper, Hepatitis, Parvovirus, and Parainfluenza", petId);
            addVaccine(vaccines, "Bordetella", "Recommended for dogs that interact with other dogs", petId);
            addVaccine(vaccines, "Leptospirosis", "Recommended for dogs at risk", petId);
        } 
        else if (petType.equalsIgnoreCase("Cat") || petType.equalsIgnoreCase("Kedi")) {
            // Core vaccines for cats
            addVaccine(vaccines, "Rabies", "Core vaccine for cats", petId);
            addVaccine(vaccines, "FVRCP", "Core vaccine protecting against Feline Viral Rhinotracheitis, Calicivirus, and Panleukopenia", petId);
            addVaccine(vaccines, "FeLV", "Recommended for outdoor cats", petId);
            addVaccine(vaccines, "FIV", "Recommended for cats at risk", petId);
        }
        
        return vaccines;
    }
    
    private static void addVaccine(List<Vaccine> vaccines, String name, String notes, long petId) {
        Vaccine vaccine = new Vaccine();
        vaccine.setName(name);
        vaccine.setNotes(notes);
        vaccine.setDateAdministered(null);
        vaccine.setNextDueDate(null);  // Don't set next due date until administered
        vaccine.setPetId(petId);
        vaccines.add(vaccine);
    }
} 