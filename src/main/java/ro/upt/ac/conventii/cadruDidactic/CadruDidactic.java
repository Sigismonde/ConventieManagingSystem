package ro.upt.ac.conventii.cadruDidactic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class CadruDidactic
{
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	private String nume;
	private String prenume;

	private String functie;
	private String specializare;

	private String telefon;
	private String email;
	
	// Adăugăm câmpul pentru semnătură (nu obligatoriu)
	@Lob
    @Column(name = "semnatura", columnDefinition="LONGBLOB")
    private byte[] semnatura;
	
	public CadruDidactic() 
	{
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public String getNume()
	{
		return nume;
	}

	public void setNume(String nume)
	{
		this.nume = nume;
	}

	public String getPrenume()
	{
		return prenume;
	}
	
	 public String getNumeComplet() {
	        return this.nume + " " + this.prenume;
	    }

	public void setPrenume(String prenume)
	{
		this.prenume = prenume;
	}

	public String getFunctie()
	{
		return functie;
	}

	public void setFunctie(String functie)
	{
		this.functie = functie;
	}

	public String getTelefon()
	{
		return telefon;
	}

	public void setTelefon(String telefon)
	{
		this.telefon = telefon;
	}

	public String getEmail()
	{
		return email;
	}

	public void setEmail(String email)
	{
		this.email = email;
	}

	public String getSpecializare()
	{
		return specializare;
	}

	public void setSpecializare(String specializare)
	{
		this.specializare = specializare;
	}
	
	// Getteri și setteri pentru semnătură
	public byte[] getSemnatura() {
        return semnatura;
    }
    
    public void setSemnatura(byte[] semnatura) {
        this.semnatura = semnatura;
    }
}