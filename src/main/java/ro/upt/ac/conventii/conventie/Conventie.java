package ro.upt.ac.conventii.conventie;

import java.sql.Date;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import ro.upt.ac.conventii.cadruDidactic.CadruDidactic;
import ro.upt.ac.conventii.companie.*;
import ro.upt.ac.conventii.student.Student;
import ro.upt.ac.conventii.tutore.Tutore;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Entity
public class Conventie 
{
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	private int id;
	
	 @ManyToOne  
	 @JoinColumn(name = "companie_id")
	private Companie companie;

	 @ManyToOne
	    @JoinColumn(name = "student_id")
	    private Student student;
	
	private String loculDesfasurarii;
	private Integer durataInPlanulDeInvatamant;
	private Date dataInceput;
	private Date dataSfarsit;
	private String numeProiect;
	@ManyToOne
	@JoinColumn(name = "tutore_id")
	private Tutore tutore;
	@ManyToOne  
    @JoinColumn(name = "cadru_didactic_id")
	private CadruDidactic cadruDidactic;
	
	private int numarCredite;
	private String indemnizatii;
	private String avantaje;
	private String altePrecizari;
	private Date dataIntocmirii;
	
	@Enumerated(EnumType.STRING)
    private ConventieStatus status = ConventieStatus.IN_ASTEPTARE_PARTENER;

	
	public Conventie()
	{
	}
	
	   public ConventieStatus getStatus() {
	        return status;
	    }

	    public void setStatus(ConventieStatus status) {
	        this.status = status;
	    }
	
	   
	 // Adăugare în clasa Conventie.java
	    private boolean trimisaTutore;

	    public boolean isTrimisaTutore() {
	        return trimisaTutore;
	    }

	    public void setTrimisaTutore(boolean trimisaTutore) {
	        this.trimisaTutore = trimisaTutore;
	    }
	
	public int getId()
	{
		return id;
	}
	
	public void setId(int id)
	{
		this.id = id;
	}

	public Companie getCompanie()
	{
		return companie;
	}

	public void setCompanie(Companie companie)
	{
		this.companie = companie;
	}

	public Student getStudent()
	{
		return student;
	}

	public void setStudent(Student student)
	{
		this.student = student;
	}

	public String getLoculDesfasurarii()
	{
		return loculDesfasurarii;
	}

	public void setLoculDesfasurarii(String loculDesfasurarii)
	{
		this.loculDesfasurarii = loculDesfasurarii;
	}

	public Integer getDurataInPlanulDeInvatamant()
	{
		return durataInPlanulDeInvatamant;
	}

	public void setDurataInPlanulDeInvatamant(Integer durataInPlanulDeInvatamant)
	{
		this.durataInPlanulDeInvatamant = durataInPlanulDeInvatamant;
	}

	public Date getDataInceput()
	{
		return dataInceput;
	}

	public void setDataInceput(Date dataInceput)
	{
		this.dataInceput = dataInceput;
	}

	public Date getDataSfarsit()
	{
		return dataSfarsit;
	}

	public void setDataSfarsit(Date dataSfarsit)
	{
		this.dataSfarsit = dataSfarsit;
	}

	public String getNumeProiect()
	{
		return numeProiect;
	}

	public void setNumeProiect(String numeProiect)
	{
		this.numeProiect = numeProiect;
	}

	public Tutore getTutore()
	{
		return tutore;
	}

	public void setTutore(Tutore tutore)
	{
		this.tutore = tutore;
	}

	public CadruDidactic getCadruDidactic()
	{
		return cadruDidactic;
	}

	public void setCadruDidactic(CadruDidactic cadruDidactic)
	{
		this.cadruDidactic = cadruDidactic;
	}

	public int getNumarCredite()
	{
		return numarCredite;
	}

	public void setNumarCredite(int numarCredite)
	{
		this.numarCredite = numarCredite;
	}

	public String getIndemnizatii()
	{
		return indemnizatii;
	}

	public void setIndemnizatii(String indemnizatii)
	{
		this.indemnizatii = indemnizatii;
	}

	public String getAvantaje()
	{
		return avantaje;
	}

	public void setAvantaje(String avantaje)
	{
		this.avantaje = avantaje;
	}

	public String getAltePrecizari()
	{
		return altePrecizari;
	}

	public void setAltePrecizari(String altePrecizari)
	{
		this.altePrecizari = altePrecizari;
	}

	public Date getDataIntocmirii()
	{
		return dataIntocmirii;
	}

	public void setDataIntocmirii(Date dataIntocmirii)
	{
		this.dataIntocmirii = dataIntocmirii;
	}	
}
