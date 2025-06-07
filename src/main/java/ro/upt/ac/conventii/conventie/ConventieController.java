
package ro.upt.ac.conventii.conventie;

import java.sql.Date;
import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import ro.upt.ac.conventii.cadruDidactic.CadruDidacticRepository;
import ro.upt.ac.conventii.companie.CompanieRepository;
import ro.upt.ac.conventii.student.StudentRepository;

@Controller
public class ConventieController
{
	@Autowired
	ConventieRepository conventieRepository;
	@Autowired
	CompanieRepository companieRepository;
	@Autowired
	CadruDidacticRepository cadruDidacticRepository;
	@Autowired
    StudentRepository studentRepository;
	
//	@GetMapping("/")
//	public String root()
//	{
//		return "index";
//	}
	
	@GetMapping("/conventie-create")
	public String create(Conventie conventie, Model model) {
	    conventie.setStatus(ConventieStatus.IN_ASTEPTARE_PARTENER);
	    
	    model.addAttribute("conventie", conventie);
	    model.addAttribute("companii", companieRepository.findAll());
	    model.addAttribute("studenti", studentRepository.findAll());
	    model.addAttribute("cadreDidactice", cadruDidacticRepository.findAll());
	    
	    return "conventie-create";
	}

	    @PostMapping("/conventie-create-save")
	    public String createSave(@Validated Conventie conventie, BindingResult result, Model model) {
	        if(result.hasErrors()) {
	            return "conventie-create";
	        }
	        conventie.setStatus(ConventieStatus.IN_ASTEPTARE_PARTENER);
	        conventieRepository.save(conventie);
	        return "redirect:/conventie-read";
	    }
	
	@GetMapping("/conventie-read")
	public String read(Model model) 
	{
		 List<Conventie> conventii = conventieRepository.findAll();
	        // Dacă nu există convenții, vom trimite o listă goală în loc de null
	        if (conventii == null) {
	            conventii = new ArrayList<>();
	        }
	        model.addAttribute("conventii", conventii);
	        return "conventie-read";
	}
	
	@GetMapping("/conventie-edit/{id}")
	public String edit(@PathVariable("id") int id, Model model) 
	{
	    Conventie conventie = conventieRepository.findById(id);
	    //.orElseThrow(() -> new IllegalArgumentException("Invalid conventie Id:" + id));
	    	    
	    model.addAttribute("conventie", conventie);
	    model.addAttribute("companii", companieRepository.findAll());
	    model.addAttribute("cadreDidactice", cadruDidacticRepository.findAll());
	    return "conventie-update";
	}
	
	@PostMapping("/conventie-update/{id}")
	public String update(@PathVariable("id") int id, @Validated Conventie conventie, BindingResult result, Model model) 
	{
	    if(result.hasErrors()) 
	    {
	        conventie.setId(id);
	        return "conventie-update";
	    }
	        
	    conventieRepository.save(conventie);
	    return "redirect:/conventie-read";
	}
	
	@GetMapping("/conventie-delete/{id}")
	public String delete(@PathVariable("id") int id, Model model) 
	{
	    Conventie conventie = conventieRepository.findById(id);
	    //.orElseThrow(() -> new IllegalArgumentException("Invalid conventie Id:" + id));
	    
	    conventieRepository.delete(conventie);
	    return "redirect:/conventie-read";
	}	
	
	
	@GetMapping("/conventie-export/{id}")
	public ResponseEntity<String> exportConventie(@PathVariable("id") int id) {
	    Conventie conventie = conventieRepository.findById(id);
	    if (conventie == null) {
	        return ResponseEntity.notFound().build();
	    }

	    String filename = "conventie_" + id + ".html";
	    if (conventie.getStudent() != null && conventie.getCompanie() != null) {
	        filename = "conventie_" + conventie.getStudent().getNume() + "_" + 
	                  conventie.getCompanie().getNume() + ".html";
	    }

	    String htmlContent = generateConventieHtml(conventie);

	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.TEXT_HTML);
	    headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);

	    return new ResponseEntity<>(htmlContent, headers, HttpStatus.OK);
	}

	private String generateConventieHtml(Conventie conventie) {
	    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
	    StringBuilder html = new StringBuilder();
	    html.append("<!DOCTYPE html>\n")
	        .append("<html>\n")
	        .append("<head>\n")
	        .append("<meta charset=\"UTF-8\">\n")
	        .append("<title>Conventie de practica</title>\n")
	        .append("<style>\n")
	        .append("body { font-family: Arial, sans-serif; margin: 40px; line-height: 1.6; }\n")
	        .append("h1, h2 { text-align: center; }\n")
	        .append("h3 { margin-top: 20px; }\n")
	        .append(".header { text-align: right; margin-bottom: 20px; }\n")
	        .append(".content { margin: 20px 0; }\n")
	        .append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n")
	        .append("table, th, td { border: 1px solid black; }\n")
	        .append("th, td { padding: 8px; text-align: left; }\n")
	        .append(".signature-table { border: none; }\n")
	        .append(".signature-table td { border: none; text-align: center; padding: 20px; }\n")
	        .append("</style>\n")
	        .append("</head>\n")
	        .append("<body>\n");

	    // Header
	    html.append("<div class=\"header\">")
	        .append("<p><strong>ANEXA 3</strong></p>")
	        .append("Nr. _____ / ").append(dateFormat.format(new java.util.Date()))
	        .append("</div>");

	    // Titlu
	    html.append("<h1>CONVENȚIE-CADRU</h1>")
	        .append("<h2>privind efectuarea stagiului de practică în cadrul programelor de studii universitare<br>")
	        .append("de licență sau masterat</h2>");

	    // Părți contractante
	    html.append("<div class=\"content\">")
	        .append("<p>Prezenta convenție-cadru se încheie între:</p>")
	        .append("<p><strong>1. Universitatea Politehnica Timișoara</strong>, reprezentată de Rector, ")
	        .append("conf. univ. dr. ing. Florin DRĂGAN, cu sediul în TIMIŞOARA, Piața Victoriei, Nr. 2, ")
	        .append("cod 300006, telefon: 0256-403011, email: rector@upt.ro, ")
	        .append("cod unic de înregistrare: 4269282, denumită în continuare <strong>organizator de practică</strong>,</p>");

	    // Companie
	    html.append("<p><strong>2. ").append(conventie.getCompanie().getNume()).append("</strong>, ")
	        .append("reprezentată de ").append(conventie.getCompanie().getReprezentant())
	        .append(" în calitate de ").append(conventie.getCompanie().getCalitate())
	        .append(", cu sediul în ").append(conventie.getCompanie().getAdresa())
	        .append(", telefon ").append(conventie.getCompanie().getTelefon())
	        .append(", denumită în continuare <strong>partener de practică</strong></p>");

	    // Student
	    html.append("<p><strong>3. Student ").append(conventie.getStudent().getNume())
	        .append(" ").append(conventie.getStudent().getPrenume()).append("</strong>, ")
	        .append("CNP ").append(conventie.getStudent().getCnp())
	        .append(", data nașterii ").append(dateFormat.format(conventie.getStudent().getDataNasterii()))
	        .append(", locul nașterii ").append(conventie.getStudent().getLoculNasterii())
	        .append(", cetățenie ").append(conventie.getStudent().getCetatenie())
	        .append(", CI seria ").append(conventie.getStudent().getSerieCi())
	        .append(" nr. ").append(conventie.getStudent().getNumarCi())
	        .append(", adresa ").append(conventie.getStudent().getAdresa())
	        .append(", înscris în anul universitar ").append(conventie.getStudent().getAnUniversitar())
	        .append(", Universitatea Politehnica Timișoara, ")
	        .append("facultatea ").append(conventie.getStudent().getFacultate())
	        .append(", specializarea ").append(conventie.getStudent().getSpecializare())
	        .append(", anul de studiu ").append(conventie.getStudent().getAnDeStudiu())
	        .append(", email ").append(conventie.getStudent().getEmail())
	        .append(", telefon ").append(conventie.getStudent().getTelefon())
	        .append(", denumit în continuare <strong>practicant</strong></p></div>");

	 // Articolul 1
	    html.append("<h3>Art. 1. Obiectul convenției-cadru</h3>")
	        .append("<p>(1) <em>Convenția-cadru</em> stabilește modul în care se organizează și se ")
	        .append("desfășoară stagiul de practică în vederea consolidării cunoștințelor teoretice și ")
	        .append("formarea abilităților practice, spre a le aplica în concordanță cu specializarea pentru ")
	        .append("care se instruiește studentul practicant.</p>")
	        .append("<p>(2) Stagiul de practică este realizat de practicant în vederea dobândirii ")
	        .append("competențelor profesionale menționate în Portofoliul de practică care este corelat cu fișa disciplinei de practică, ")
	        .append("parte integrantă a prezentei convenții. ")
	        .append("Locul desfășurării stagiului de practică este: ").append(conventie.getLoculDesfasurarii()).append("</p>")
	        .append("<p>(3) Modalitățile de derulare și conținutul stagiului de practică sunt descrise în ")
	        .append("prezenta convenție-cadru și în portofoliul de practică din anexă.</p>");

	    // Articolul 2
	    html.append("<h3>Art. 2. Statutul practicantului</h3>")
	        .append("<p>Practicantul rămâne, pe toată durata stagiului de pregătire practică, student al ")
	        .append("Universității Politehnica Timișoara.</p>");

	    // Articolul 3
	    html.append("<h3>Art. 3. Durata și perioada desfășurării stagiului de practică</h3>")
	        .append("<p>(1) Durata stagiului de practică, precizată în planul de învățământ, este de ")
	        .append(conventie.getDurataInPlanulDeInvatamant()).append(" [h].</p>")
	        .append("<p>(2) Perioada desfășurării stagiului de practică este conformă structurii anului universitar curent ")
	        .append("de la ").append(dateFormat.format(conventie.getDataInceput()))
	        .append(" până la ").append(dateFormat.format(conventie.getDataSfarsit())).append("</p>");

	    // Articolul 4
	    html.append("<h3>Art. 4. Plata și obligațiile sociale</h3>")
	        .append("<p>(1) Stagiul de pregătire practică (se bifează situația corespunzătoare):</p>")
	        .append("<p>☐ - se efectuează în cadrul unui contract de muncă, cei doi parteneri putând să beneficieze ")
	        .append("de prevederile Legii nr. 72/2007 privind stimularea încadrării în muncă a elevilor și studenților;</p>")
	        .append("<p>☐ - nu se efectuează în cadrul unui contract de muncă;</p>")
	        .append("<p>☐ - se efectuează în cadrul unui proiect finanțat prin Fondul Social European;</p>")
	        .append("<p>☐ - se efectuează în cadrul proiectului ...</p>")
	        .append("<p>(2) În cazul angajării ulterioare, perioada stagiului nu va fi considerată ca vechime ")
	        .append("în muncă în situația în care convenția nu se derulează în cadrul unui contract de muncă.</p>")
	        .append("<p>(3) Practicantul nu poate pretinde un salariu din partea partenerului de practică, cu ")
	        .append("excepția situației în care practicantul are statut de angajat.</p>")
	        .append("<p>(4) Partenerul de practică poate totuși acorda practicantului o indemnizație, ")
	        .append("gratificare, primă sau avantaje în natură, conform legislației în vigoare.</p>");

	    // Articolul 5
	    html.append("<h3>Art. 5. Responsabilitățile practicantului</h3>")
	        .append("<p>(1) Practicantul are obligația, ca pe durata derulării stagiului de practică, să ")
	        .append("respecte programul de lucru stabilit și să execute activitățile specificate de tutore ")
	        .append("în conformitate cu portofoliul de practică, în condițiile respectării cadrului legal cu ")
	        .append("privire la volumul și dificultatea acestora.</p>")
	        .append("<p>(2) Pe durata stagiului, practicantul respectă regulamentul de ordine interioară al ")
	        .append("partenerului de practică. În cazul nerespectării acestui regulament, conducătorul ")
	        .append("partenerului de practică își rezervă dreptul de a anula convenția-cadru, după ce în ")
	        .append("prealabil a ascultat punctul de vedere al practicantului și al îndrumătorului de ")
	        .append("practică și a înștiințat conducătorul facultății unde practicantul este înmatriculat ")
	        .append("și după primirea confirmării de primire a acestei informații. Această situație conduce la refacerea stagiului de practică și la susținerea unui nou colocviu de evaluare în anul universitar următor.</p>")
	        .append("<p>(3) Practicantul are obligația de a respecta normele de securitate și sănătate în ")
	        .append("muncă pe care le-a însușit de la reprezentantul partenerului de practică înainte de ")
	        .append("începerea stagiului de practică.</p>")
	        .append("<p>(4) Practicantul se angajează să nu folosească, în niciun caz, informațiile la care ")
	        .append("are acces în timpul stagiului despre partenerul de practică sau clienții săi, pentru a ")
	        .append("le comunica unui terț sau pentru a le publica, chiar după terminarea stagiului, decât ")
	        .append("cu acordul respectivului partener de practică.</p>");

	   
	 // Articolul 6
	    html.append("<h3>Art. 6. Responsabilitățile partenerului de practică</h3>")
	        .append("<p>(1) Partenerul de practică va stabili un <em>tutore pentru stagiul de practică</em>, ")
	        .append("selectat dintre salariații proprii și ale cărui obligații sunt menționate în portofoliul ")
	        .append("de practică, parte integrantă a convenției-cadru.</p>")
	        .append("<p>(2) În cazul nerespectării obligațiilor de către practicant, tutorele va contacta ")
	        .append("cadrul didactic supervizor, responsabil de practică, aplicându-se sancțiuni conform ")
	        .append("legilor și regulamentelor în vigoare.</p>")
	        .append("<p>(3) Înainte de începerea stagiului de practică, partenerul are obligația de a face ")
	        .append("practicantului instructajul cu privire la normele de securitate și sănătate în muncă, ")
	        .append("pentru fiecare loc distinct de practică, în conformitate cu legislația în vigoare. ")
	        .append("Printre responsabilitățile sale, partenerul de practică va lua măsurile necesare pentru ")
	        .append("securitatea și sănătatea în muncă a practicantului, precum și pentru comunicarea ")
	        .append("regulilor de prevenire a riscurilor profesionale.</p>")
	        .append("<p>(4) Partenerul de practică trebuie să pună la dispoziția practicantului toate ")
	        .append("mijloacele necesare pentru desfășurarea activităților precizate în portofoliul de practică.</p>")
	        .append("<p>(5) Partenerul de practică are obligația de a asigura practicantului accesul liber ")
	        .append("la serviciul de medicina muncii, pe durata derulării pregătirii practice.</p>")
	        .append("<p>(6) În urma desfășurării cu succes a stagiului, partenerul de practică va acorda ")
	        .append("studentului, la cerere, o adeverință constatatoare.</p>");

	    // Articolul 7
	    html.append("<h3>Art. 7. Obligațiile organizatorului de practică</h3>")
	        .append("<p>(1) Organizatorul de practică desemnează un cadru didactic supervizor, responsabil ")
	        .append("cu planificarea, organizarea și supravegherea desfășurării pregătirii practice. ")
	        .append("Cadrul didactic supervizor responsabil de practică, împreună cu tutorele desemnat de ")
	        .append("partenerul de practică stabilesc tematica de practică și competențele profesionale ")
	        .append("care fac obiectul stagiului de pregătire practică.</p>")
	        .append("<p>(2) În cazul în care derularea stagiului de pregătire practică nu este conformă cu ")
	        .append("angajamentele luate de către partenerul de practică în cadrul prezentei convenții, ")
	        .append("conducătorul organizatorului de practică poate decide întreruperea stagiului de ")
	        .append("pregătire practică conform convenției-cadru, după informarea prealabilă a ")
	        .append("conducătorului partenerului de practică și după primirea confirmării de primire a ")
	        .append("acestei informații.</p>");

	    // Articolul 8
	    html.append("<h3>Art. 8. Persoane desemnate de organizatorul de practică și partenerul de practică</h3>")
	        .append("<p>(1) <strong>Tutorele</strong> (persoana care va avea responsabilitatea practicantului din partea partenerului de practică):</p>")
	        .append("<p>Dl/Dna ").append(conventie.getTutore().getNume()).append(" ").append(conventie.getTutore().getPrenume()).append("<br>")
	        .append("Funcția: ").append(conventie.getTutore().getFunctie()).append("<br>")
	        .append("Telefon: ").append(conventie.getTutore().getTelefon()).append("<br>")
	        .append("Email: ").append(conventie.getTutore().getEmail()).append("</p>")
	        .append("<p>(2) <strong>Cadrul didactic supervizor</strong>, responsabil cu urmărirea derulării stagiului de practică din partea organizatorului de practică:</p>")
	        .append("<p>Dl/Dna ").append(conventie.getCadruDidactic().getNume()).append(" ").append(conventie.getCadruDidactic().getPrenume()).append("<br>")
	        .append("Funcția: ").append(conventie.getCadruDidactic().getFunctie()).append("<br>")
	        .append("Telefon: ").append(conventie.getCadruDidactic().getTelefon()).append("<br>")
	        .append("Email: ").append(conventie.getCadruDidactic().getEmail()).append("</p>");

	    // Articolul 9
	    html.append("<h3>Art. 9. Evaluarea stagiului de pregătire practică prin credite transferabile</h3>")
	        .append("<p>Numărul de credite transferabile ce vor fi obținute în urma desfășurării stagiului ")
	        .append("de practică este de ").append(conventie.getNumarCredite()).append(".</p>");
	 // Articolul 10
	    html.append("<h3>Art. 10. Raportul privind stagiul de pregătire practică</h3>")
	        .append("<p>(1) În timpul derulării stagiului de practică, tutorele, împreună cu cadrul ")
	        .append("didactic supervizor, vor evalua practicantul în permanență. Vor fi monitorizate și ")
	        .append("evaluate atât nivelul de dobândire a competențelor profesionale, cât și ")
	        .append("comportamentul și modalitatea de integrare a practicantului în activitatea ")
	        .append("partenerului de practică (disciplină, punctualitate, responsabilitate în rezolvarea ")
	        .append("sarcinilor, respectarea regulamentului de ordine interioară al partenerului de practică).</p>")
	        .append("<p>(2) La finalul stagiului de practică, tutorele completează atestatul de practică ")
	        .append("și opțional fișa de evaluare, pe baza evaluării nivelului de dobândire a ")
	        .append("competențelor de către practicant. Rezultatul acestei evaluări va sta la baza notării ")
	        .append("practicantului de către cadrul didactic supervizor.</p>")
	        .append("<p>La finalul stagiului de practică, studentul elaborează un caiet de practică, ")
	        .append("însușit și de tutorele din partea partenerului de practică. Atestatul și fișa de ")
	        .append("evaluare completate de tutore vor sta la baza notării studentului conform ")
	        .append("<em>Regulamentului cadru de organizare și desfășurare a practicii studenților în UPT.</em></p>")
	        .append("<p>(3) Periodic și după încheierea stagiului de practică, practicantul va prezenta un ")
	        .append("<em>caiet de practică</em> care va cuprinde:</p>")
	        .append("<ul>")
	        .append("<li>denumirea modulului de pregătire;</li>")
	        .append("<li>competențe exersate;</li>")
	        .append("<li>activități desfășurate pe perioada stagiului de practică;</li>")
	        .append("<li>observații personale privitoare la activitatea depusă.</li>")
	        .append("</ul>")
	        .append("<p>(4) Pentru studiile de licență, în urma unui colocviu susținut în instituția de ")
	        .append("învățământ superior, pe baza documentelor de practică, calificativul foarte bine/ ")
	        .append("bine/ satisfăcător emis de instituția gazdă se omologhează cu calificativul ")
	        .append("<em>promovat</em> în catalogul disciplinei practică, iar calificativul nesatisfăcător ")
	        .append("emis de instituția gazdă se omologhează cu calificativul <em>nepromovat</em> în ")
	        .append("catalogul disciplinei practică.</p>")
	        .append("<p>(5) Pentru studiile de master, în urma unui colocviu susținut în instituția de ")
	        .append("învățământ superior, pe baza documentelor de practică, calificativul foarte bine/ ")
	        .append("bine/ satisfăcător emis de instituția gazdă se echivalează cu note de promovare în ")
	        .append("catalogul disciplinei practică (5-10), iar calificativul nesatisfăcător emis de ")
	        .append("instituția gazdă se echivalează cu note de nepromovare în catalogul disciplinei practică.</p>");

	    // Articolul 11
	    html.append("<h3>Art. 11. Sănătatea și securitatea în muncă</h3>")
	        .append("<p>(1) Practicantul anexează prezentului contract dovada asigurării medicale ")
	        .append("valabile în perioada și pe teritoriul statului unde se desfășoară stagiul de practică.</p>")
	        .append("<p>(2) Partenerul de practică are obligația respectării prevederilor legale cu ")
	        .append("privire la sănătatea și securitatea în muncă a practicatului pe durata stagiului de practică.</p>")
	        .append("<p>(3) Practicantului i se asigură protecție socială conform legislației în vigoare. ")
	        .append("Ca urmare, conform dispozițiilor Legii nr. 346/2002 privind asigurările pentru ")
	        .append("accidente de muncă și boli profesionale, cu modificările și completările ulterioare, ")
	        .append("practicantul beneficiază de legislația privitoare la accidentele de muncă pe toată ")
	        .append("durata efectuării pregătirii practice.</p>")
	        .append("<p>(4) În cazul unui accident suferit de practicant, fie în cursul lucrului, fie în ")
	        .append("timpul deplasării la lucru, partenerul de practică se angajează să înștiințeze ")
	        .append("asiguratorul cu privire la accidentul care a avut loc.</p>");

	    // Articolul 12
	    html.append("<h3>Art. 12. Condiții facultative de desfășurare a stagiului de pregătire practică</h3>")
	        .append("<p>(1) Îndemnizație, gratificări sau prime acordate practicantului:</p>")
	        .append("<p>").append(conventie.getIndemnizatii()).append("</p>")
	        .append("<p>(2) Avantaje eventuale (plata transportului de la și la locul desfășurării stagiului de practică, ")
	        .append("tichete de masă, acces la cantina partenerului de practică etc.):</p>")
	        .append("<p>").append(conventie.getAvantaje()).append("</p>")
	        .append("<p>(3) Alte precizări:</p>")
	        .append("<p>").append(conventie.getAltePrecizari()).append("</p>");

	    // Articolul 13 și semnături
	    html.append("<h3>Art. 13. Prevederi finale</h3>")
	        .append("<p>Întocmit în trei exemplare la data: ")
	        .append(dateFormat.format(conventie.getDataIntocmirii())).append("</p>")
	        .append("<table class='signature-table'>")
	        .append("<tr>")
	        .append("<th>Universitatea Politehnica Timișoara, prin Rector</th>")
	        .append("<th>Partener de practică, prin Reprezentant</th>")
	        .append("<th>Practicant</th>")
	        .append("</tr>")
	        .append("<tr>")
	        .append("<td>Conf. univ. dr. ing.<br>Florin DRĂGAN</td>")
	        .append("<td>").append(conventie.getCompanie().getReprezentant()).append("</td>")
	        .append("<td>").append(conventie.getStudent().getNume()).append(" ")
	        .append(conventie.getStudent().getPrenume()).append("</td>")
	        .append("</tr>")
	        .append("<tr>")
	        .append("<td>Data _____</td>")
	        .append("<td>Data _____</td>")
	        .append("<td>Data _____</td>")
	        .append("</tr>")
	        .append("<tr>")
	        .append("<td>Semnătura _____</td>")
	        .append("<td>Semnătura _____</td>")
	        .append("<td>Semnătura _____</td>")
	        .append("</tr>")
	        .append("</table>");

	    // Am luat la cunoștință
	    html.append("<p>Am luat la cunoștință,</p>")
	        .append("<table class='signature-table'>")
	        .append("<tr>")
	        .append("<th>Cadru didactic supervizor</th>")
	        .append("<th>Tutore</th>")
	        .append("</tr>")
	        .append("<tr>")
	        .append("<td>Nume și prenume: ").append(conventie.getCadruDidactic().getNume()).append(" ")
	        .append(conventie.getCadruDidactic().getPrenume()).append("</td>")
	        .append("<td>Nume și prenume: ").append(conventie.getTutore().getNume()).append(" ")
	        .append(conventie.getTutore().getPrenume()).append("</td>")
	        .append("</tr>")
	        .append("<tr>")
	        .append("<td>Funcția: ").append(conventie.getCadruDidactic().getFunctie()).append("</td>")
	        .append("<td>Funcția: ").append(conventie.getTutore().getFunctie()).append("</td>")
	        .append("</tr>")
	        .append("<tr>")
	        .append("<td>Data _____</td>")
	        .append("<td>Data _____</td>")
	        .append("</tr>")
	        .append("<tr>")
	        .append("<td>Semnătura _____</td>")
	        .append("<td>Semnătura _____</td>")
	        .append("</tr>")
	        .append("</table>");
	 // ANEXA - Portofoliul de practică
	    html.append("<h2>ANEXĂ la Convenția-cadru</h2>")
	        .append("<h3>PORTOFOLIU DE PRACTICĂ</h3>")
	        .append("<p>aferent <strong>Convenției-cadru</strong> privind efectuarea stagiului de practică în cadrul ")
	        .append("programelor de studii universitare de licență/masterat</p>")
	        
	        .append("<p><strong>1.</strong> Durata totală a pregătirii practice: ")
	        .append(conventie.getDurataInPlanulDeInvatamant()).append("</p>")
	        
	        .append("<p><strong>2.</strong> Calendarul pregătirii:<br>")
	        .append("De la ").append(dateFormat.format(conventie.getDataInceput()))
	        .append(" până la ").append(dateFormat.format(conventie.getDataSfarsit()))
	        .append("</p>")
	        
	        .append("<p><strong>3.</strong> Perioada stagiului, timpul de lucru și orarul (de precizat zilele de pregătire practică în cazul ")
	        .append("timpului de lucru parțial): _____</p>")
	        
	        .append("<p><strong>4.</strong> Adresa unde se va derula stagiul de pregătire practică: ")
	        .append(conventie.getLoculDesfasurarii()).append("</p>")
	        
	        .append("<p><strong>5.</strong> Deplasarea în afara locului unde este repartizat practicantul vizează următoarele ")
	        .append("locații: _____</p>")
	        
	        .append("<p><strong>6.</strong> Condiții de primire a studentului/masterandului în stagiul de practică:<br>")
	        .append("_____</p>")
	        
	        .append("<p><strong>7.</strong> Modalități prin care se asigură complementaritatea între pregătirea dobândită de ")
	        .append("studentul practicant în instituția de învățământ superior și în cadrul stagiului de practică:<br>")
	        .append("_____</p>")
	        
	        .append("<p><strong>8.</strong> Numele și prenumele cadrului didactic care asigură supravegherea pedagogică a ")
	        .append("practicantului pe perioada stagiului de practică: ")
	        .append(conventie.getCadruDidactic().getNume()).append(" ")
	        .append(conventie.getCadruDidactic().getPrenume()).append("</p>")
	        
	        .append("<p><strong>9.</strong> Drepturi și responsabilități ale cadrului didactic din unitatea de învățământ - ")
	        .append("organizator al practicii, pe perioada stagiului de practică:<br>")
	        .append("_____</p>")
	        
	        .append("<p><strong>10.</strong> Numele și prenumele tutorelui desemnat de întreprindere care va asigura ")
	        .append("respectarea condițiilor de pregătire și dobândirea de către practicant a competențelor ")
	        .append("profesionale planificate pentru perioada stagiului de practică: ")
	        .append(conventie.getTutore().getNume()).append(" ")
	        .append(conventie.getTutore().getPrenume()).append("</p>")
	        
	        .append("<p><strong>11.</strong> Drepturi și responsabilități ale tutorelui de practică desemnat de partenerul ")
	        .append("de practică:<br>")
	        .append("_____</p>")
	        
	        .append("<p><strong>12.</strong> Modalitățile de derulare a stagiului de practică - Tematica practicii și ")
	        .append("sarcinile studentului conform prevederilor din Fișa disciplinei a stagiului de practică:<br>")
	        .append("_____</p>")
	        
	        .append("<p><strong>13.</strong> Definirea competențelor care vor fi dobândite pe perioada stagiului de practică</p>")
	        .append("<table border='1'>")
	        .append("<tr>")
	        .append("<td><strong>Competențe</strong></td>")
	        .append("<td>_____</td>")
	        .append("</tr>")
	        .append("<tr>")
	        .append("<td><strong>Modulul de pregătire</strong></td>")
	        .append("<td>_____</td>")
	        .append("</tr>")
	        .append("<tr>")
	        .append("<td><strong>Locul de muncă</strong></td>")
	        .append("<td>_____</td>")
	        .append("</tr>")
	        .append("<tr>")
	        .append("<td><strong>Activități planificate</strong></td>")
	        .append("<td>_____</td>")
	        .append("</tr>")
	        .append("<tr>")
	        .append("<td><strong>Observații</strong></td>")
	        .append("<td>_____</td>")
	        .append("</tr>")
	        .append("</table>")
	        
	        .append("<p><strong>14.</strong> Modalități de evaluare a pregătirii profesionale dobândite de practicant pe ")
	        .append("perioada stagiului de pregătire practică:<br>")
	        .append("Evaluarea practicantului pe perioada stagiului de pregătire practică se va face de către tutore.</p>");

	    // Final semnături pentru anexă
	    html.append("<table class='signature-table'>")
	        .append("<tr>")
	        .append("<th>Cadru didactic supervizor</th>")
	        .append("<th>Tutore</th>")
	        .append("<th>Practicant</th>")
	        .append("</tr>")
	        .append("<tr>")
	        .append("<td>Nume și prenume:<br>").append(conventie.getCadruDidactic().getNume()).append(" ")
	        .append(conventie.getCadruDidactic().getPrenume()).append("</td>")
	        .append("<td>Nume și prenume:<br>").append(conventie.getTutore().getNume()).append(" ")
	        .append(conventie.getTutore().getPrenume()).append("</td>")
	        .append("<td>Nume și prenume:<br>").append(conventie.getStudent().getNume()).append(" ")
	        .append(conventie.getStudent().getPrenume()).append("</td>")
	        .append("</tr>")
	        .append("<tr>")
	        .append("<td>Funcția: ").append(conventie.getCadruDidactic().getFunctie()).append("</td>")
	        .append("<td>Funcția: ").append(conventie.getTutore().getFunctie()).append("</td>")
	        .append("<td>Student</td>")
	        .append("</tr>")
	        .append("<tr>")
	        .append("<td>Data _____</td>")
	        .append("<td>Data _____</td>")
	        .append("<td>Data _____</td>")
	        .append("</tr>")
	        .append("<tr>")
	        .append("<td>Semnătura _____</td>")
	        .append("<td>Semnătura _____</td>")
	        .append("<td>Semnătura _____</td>")
	        .append("</tr>")
	        .append("</table>");

	    // Nota de subsol
	    html.append("<p style='font-size: 0.8em;'>Punctele 12 și 13 se vor completa de către cadrul didactic ")
	        .append("supervizor conform fișei de disciplină a stagiului de practică.</p>");

	    html.append("</body></html>");

	    return html.toString();
	 }
}


