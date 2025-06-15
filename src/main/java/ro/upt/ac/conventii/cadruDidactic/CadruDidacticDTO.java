package ro.upt.ac.conventii.cadruDidactic;

import java.util.Base64;

public class CadruDidacticDTO {
    private CadruDidactic cadruDidactic;
    private String semnatura64;
    private boolean areSemnatura;
    
    public CadruDidacticDTO(CadruDidactic cadruDidactic) {
        this.cadruDidactic = cadruDidactic;
        this.areSemnatura = cadruDidactic.getSemnatura() != null;
        if (this.areSemnatura) {
            this.semnatura64 = Base64.getEncoder().encodeToString(cadruDidactic.getSemnatura());
        }
    }
    
    // Getteri
    public CadruDidactic getCadruDidactic() {
        return cadruDidactic;
    }
    
    public String getSemnatura64() {
        return semnatura64;
    }
    
    public boolean isAreSemnatura() {
        return areSemnatura;
    }
    
    // Delegare pentru proprietățile cadrului didactic
    public int getId() {
        return cadruDidactic.getId();
    }
    
    public String getNume() {
        return cadruDidactic.getNume();
    }
    
    public String getPrenume() {
        return cadruDidactic.getPrenume();
    }
    
    public String getNumeComplet() {
        return cadruDidactic.getNumeComplet();
    }
    
    public String getFunctie() {
        return cadruDidactic.getFunctie();
    }
    
    public String getSpecializare() {
        return cadruDidactic.getSpecializare();
    }
    
    public String getEmail() {
        return cadruDidactic.getEmail();
    }
    
    public String getTelefon() {
        return cadruDidactic.getTelefon();
    }
    
    public byte[] getSemnatura() {
        return cadruDidactic.getSemnatura();
    }
}