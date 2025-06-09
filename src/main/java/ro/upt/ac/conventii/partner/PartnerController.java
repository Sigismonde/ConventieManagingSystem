package ro.upt.ac.conventii.partner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Arrays;


import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
//Biblioteca iText principală
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Chunk;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.Image;

//Pentru gestionarea fluxurilor de date
import java.io.ByteArrayOutputStream;
import java.io.IOException;

//Pentru formatarea datelor
import java.text.SimpleDateFormat;
//import java.util.Date;

import jakarta.servlet.http.HttpServletResponse;
import ro.upt.ac.conventii.companie.Companie;
import ro.upt.ac.conventii.companie.CompanieRepository;
import ro.upt.ac.conventii.conventie.Conventie;
import ro.upt.ac.conventii.conventie.ConventieRepository;
import ro.upt.ac.conventii.conventie.ConventieStatus;
import ro.upt.ac.conventii.prodecan.Prodecan;
import ro.upt.ac.conventii.security.User;
import ro.upt.ac.conventii.security.UserRepository;

@Controller
@RequestMapping("/partner")
public class PartnerController {

    @Autowired
    private PartnerRepository partnerRepository;
    
    @Autowired
    private ConventieRepository conventieRepository;
    
    @Autowired
    private CompanieRepository companieRepository;
    
    @Autowired
    private UserRepository userRepository;
    
 // În PartnerController.java - actualizează aceste metode:

 // În PartnerController.java - actualizează aceste metode:

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return "redirect:/login";
        }
        
        User user = (User) authentication.getPrincipal();
        model.addAttribute("user", user);
        
        try {
            // Găsim partenerul după email
            Partner partner = partnerRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("Partner not found"));
            
            // Convenții în așteptare de aprobare de la partener
            List<Conventie> conventiiInAsteptare = conventieRepository.findByStatusAndCompanieId(
                ConventieStatus.IN_ASTEPTARE_PARTENER, partner.getCompanie().getId());
            
            // Ultimele 5 convenții aprobate de partener
            Pageable topFive = PageRequest.of(0, 5);
            List<ConventieStatus> statusuriAprobate = Arrays.asList(
                ConventieStatus.APROBATA_PARTENER,
                ConventieStatus.IN_ASTEPTARE_TUTORE,
                ConventieStatus.APROBATA_TUTORE,
                ConventieStatus.IN_ASTEPTARE_PRODECAN,
                ConventieStatus.IN_ASTEPTARE_PRORECTOR,
                ConventieStatus.APROBATA
            );
            List<Conventie> conventiiAprobate = conventieRepository.findByStatusInAndCompanieId(
                statusuriAprobate, partner.getCompanie().getId());
            
            // Limităm la ultimele 5
            if (conventiiAprobate.size() > 5) {
                conventiiAprobate = conventiiAprobate.subList(0, 5);
            }
            
            if (conventiiInAsteptare == null) conventiiInAsteptare = new ArrayList<>();
            if (conventiiAprobate == null) conventiiAprobate = new ArrayList<>();
            
            model.addAttribute("conventiiInAsteptare", conventiiInAsteptare);
            model.addAttribute("conventiiAprobate", conventiiAprobate);
            
        } catch (Exception e) {
            model.addAttribute("conventiiInAsteptare", new ArrayList<>());
            model.addAttribute("conventiiAprobate", new ArrayList<>());
            System.err.println("Error loading data: " + e.getMessage());
        }
        
        return "partner/dashboard";
    }

  
 // În PartnerController.java - actualizează metoda aprobaConventie

    @PostMapping("/aproba-conventie/{id}")
    public String aprobaConventie(@PathVariable("id") int id, 
                                 Authentication authentication, 
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            Partner partner = partnerRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("Partner not found"));
            
            // Verificăm dacă ORICE partener din companie are o semnătură încărcată
            List<Partner> partnersFromCompany = partnerRepository.findByCompanieId(partner.getCompanie().getId());
            boolean hasSignatureInCompany = false;
            
            if (partnersFromCompany != null && !partnersFromCompany.isEmpty()) {
                for (Partner p : partnersFromCompany) {
                    if (p.getSemnatura() != null) {
                        hasSignatureInCompany = true;
                        break;
                    }
                }
            }
            
            if (!hasSignatureInCompany) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Nu puteți aproba convenția fără o semnătură încărcată. Vă rugăm să încărcați mai întâi semnătura în panoul de control sau să contactați un coleg din companie care are semnătura încărcată.");
                return "redirect:/partner/conventii";
            }
            
            Conventie conventie = conventieRepository.findById(id);
            if (conventie != null) {
                // Verificăm dacă convenția aparține companiei partenerului
                if (conventie.getCompanie().getId() != partner.getCompanie().getId()) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "Nu aveți permisiunea să aprobați această convenție!");
                    return "redirect:/partner/conventii";
                }
                
                // Verificăm dacă convenția este în starea corectă
                if (conventie.getStatus() != ConventieStatus.IN_ASTEPTARE_PARTENER) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "Această convenție nu este în stadiul de așteptare aprobare de la partener!");
                    return "redirect:/partner/conventii";
                }
                
                // Verificăm dacă studentul are semnătura încărcată
                if (conventie.getStudent().getSemnatura() == null) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "Convenția nu poate fi aprobată deoarece studentul nu și-a încărcat încă semnătura digitală. " +
                        "Vă rugăm să contactați studentul să își încarce semnătura în panoul său de control.");
                    return "redirect:/partner/conventii";
                }
                
                // Actualizăm status-ul la APROBATA_PARTENER
                conventie.setStatus(ConventieStatus.APROBATA_PARTENER);
                conventie.setDataIntocmirii(new java.sql.Date(System.currentTimeMillis()));
                conventieRepository.save(conventie);
                
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Convenția a fost aprobată cu succes! Acum poate fi trimisă către tutore.");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Eroare la aprobarea convenției: " + e.getMessage());
        }
        
        return "redirect:/partner/conventii";
    }

    // Actualizează și metoda pentru încărcarea semnăturii cu validări suplimentare
    @PostMapping("/upload-semnatura")
    public String uploadSemnatura(@RequestParam("semnatura") MultipartFile file, 
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            Partner partner = partnerRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("Partner not found"));
            
            // Verificări de validare pentru fișierul încărcat
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Vă rugăm să selectați un fișier pentru încărcare.");
                return "redirect:/partner/dashboard";
            }
            
            // Verificăm tipul fișierului
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Vă rugăm să încărcați doar fișiere imagine (JPG, PNG, GIF).");
                return "redirect:/partner/dashboard";
            }
            
            // Verificăm dimensiunea fișierului (max 2MB)
            long maxSize = 2 * 1024 * 1024; // 2MB în bytes
            if (file.getSize() > maxSize) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Fișierul este prea mare. Dimensiunea maximă acceptată este de 2MB.");
                return "redirect:/partner/dashboard";
            }
            
            // Verificăm că fișierul conține date valide
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length == 0) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Fișierul selectat este corupt sau gol.");
                return "redirect:/partner/dashboard";
            }

            // Salvăm semnătura
            partner.setSemnatura(fileBytes);
            partnerRepository.save(partner);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Semnătura a fost încărcată cu succes! Acum puteți aproba convențiile.");
            
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Eroare la procesarea fișierului: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Eroare neașteptată la încărcarea semnăturii: " + e.getMessage());
        }
        
        return "redirect:/partner/dashboard";
    }

    @PostMapping("/respinge-conventie/{id}")
    public String respingeConventie(@PathVariable("id") int id, 
                                   Authentication authentication, 
                                   RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            Partner partner = partnerRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("Partner not found"));
            
            Conventie conventie = conventieRepository.findById(id);
            if (conventie != null) {
                // Verificăm dacă convenția aparține companiei partenerului
            	if (conventie.getCompanie().getId() != partner.getCompanie().getId())  {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "Nu aveți permisiunea să respingeți această convenție!");
                    return "redirect:/partner/conventii";
                }
                
                // Verificăm dacă convenția este în starea corectă
                if (conventie.getStatus() != ConventieStatus.IN_ASTEPTARE_PARTENER) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "Această convenție nu este în stadiul de așteptare aprobare de la partener!");
                    return "redirect:/partner/conventii";
                }
                
                // Actualizăm status-ul la RESPINSA
                conventie.setStatus(ConventieStatus.RESPINSA);
                conventie.setDataIntocmirii(new java.sql.Date(System.currentTimeMillis()));
                conventieRepository.save(conventie);
                
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Convenția a fost respinsă. Studentul va fi notificat și poate modifica convenția.");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Eroare la respingerea convenției: " + e.getMessage());
        }
        
        return "redirect:/partner/conventii";
    }

    
    
    // List all conventions
    @GetMapping("/conventii")
    public String conventii(Authentication authentication, Model model) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return "redirect:/login";
        }
        
        User user = (User) authentication.getPrincipal();
        model.addAttribute("user", user);
        
        try {
            Partner partner = partnerRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("Partner not found"));
            
            // TOATE convențiile companiei (pentru management complet)
            List<Conventie> conventii = conventieRepository.findByCompanieId(partner.getCompanie().getId());
            
            if (conventii == null) conventii = new ArrayList<>();
            
            model.addAttribute("conventii", conventii);
            
        } catch (Exception e) {
            model.addAttribute("conventii", new ArrayList<>());
            System.err.println("Error loading conventions: " + e.getMessage());
        }
        
        return "partner/conventii";
    }
    
    // Approve convention
  
   
    
   
    // View convention details
    @GetMapping("/conventie-view/{id}")
    public String viewConventie(@PathVariable("id") int id, 
                              Authentication authentication, 
                              Model model) {
        User user = (User) authentication.getPrincipal();
        Partner partner = partnerRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("Partner not found"));
        
        Conventie conventie = conventieRepository.findById(id);
        
        if (conventie == null || conventie.getCompanie().getId() != partner.getCompanie().getId()) {
            return "redirect:/partner/conventii";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("partner", partner);
        model.addAttribute("conventie", conventie);
        
        return "partner/conventie-view";
    }
    
 // În PartnerController.java - înlocuiește metoda exportConventie

 // În PartnerController.java - înlocuiește metoda exportConventie cu aceasta:

    @GetMapping("/conventie-export/{id}")
    public ResponseEntity<String> exportConventie(@PathVariable("id") int id, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Partner partner = partnerRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("Partner not found"));
        
        Conventie conventie = conventieRepository.findById(id);
        
        if (conventie == null || conventie.getCompanie().getId() != partner.getCompanie().getId()) {
            return ResponseEntity.notFound().build();
        }

        String filename = String.format("conventie_%s_%s.html", 
            conventie.getStudent().getNume(),
            conventie.getCompanie().getNume());

        String htmlContent = generateConventieHtml(conventie, authentication);

        // Adaugă BOM la începutul documentului pentru encoding corect
        byte[] bomBytes = new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF };
        byte[] contentBytes = htmlContent.getBytes(StandardCharsets.UTF_8);
        byte[] fullContent = new byte[bomBytes.length + contentBytes.length];
        System.arraycopy(bomBytes, 0, fullContent, 0, bomBytes.length);
        System.arraycopy(contentBytes, 0, fullContent, bomBytes.length, contentBytes.length);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.set(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8");
        
        return new ResponseEntity<>(new String(fullContent, StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }

    // Actualizează metoda generateConventieHtml pentru a fi mai simplă:
    private String generateConventieHtml(Conventie conventie, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Partner partner = partnerRepository.findByEmail(user.getEmail())
                .orElse(null);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n")
            .append("<html>\n")
            .append("<head>\n")
            .append("<meta charset=\"UTF-8\">\n")
            .append("<title>Convenție de practică</title>\n")
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
            .append("conf. univ. dr. ing. Florin DRĂGAN, cu sediul în TIMIȘOARA, Piața Victoriei, Nr. 2, ")
            .append("cod 300006, telefon: 0256-403011, email: rector@upt.ro, ")
            .append("cod unic de înregistrare: 4269282, denumită în continuare <strong>organizator de practică</strong>,</p>");

        // Companie
        if (conventie.getCompanie() != null) {
            html.append("<p><strong>2. ").append(conventie.getCompanie().getNume()).append("</strong>, ");
            
            String reprezentant = conventie.getCompanie().getReprezentant() != null ? 
                conventie.getCompanie().getReprezentant() : "N/A";
            String calitate = conventie.getCompanie().getCalitate() != null ? 
                conventie.getCompanie().getCalitate() : "N/A";
            html.append("reprezentată de ").append(reprezentant)
                .append(" în calitate de ").append(calitate);
            
            String adresa = conventie.getCompanie().getAdresa() != null ? 
                conventie.getCompanie().getAdresa() : "N/A";
            String telefon = conventie.getCompanie().getTelefon() != null ? 
                conventie.getCompanie().getTelefon() : "N/A";
            html.append(", cu sediul în ").append(adresa)
                .append(", telefon ").append(telefon);
            
            String email = conventie.getCompanie().getEmail() != null ? 
                conventie.getCompanie().getEmail() : "N/A";
            String cui = conventie.getCompanie().getCui() != null ? 
                conventie.getCompanie().getCui() : "N/A";
            html.append(", email: ").append(email)
                .append(", cod de înregistrare fiscală: ").append(cui);
            
            String nrRegCom = conventie.getCompanie().getNrRegCom() != null ? 
                conventie.getCompanie().getNrRegCom() : "N/A";
            html.append(", înregistrată la Registrul comertului cu numărul: ").append(nrRegCom);
            
            html.append(", denumită în continuare <strong>partener de practică</strong>,</p>");
        } else {
            html.append("<p><strong>2. [Companie nedefinită]</strong></p>");
        }

        // Student
        if (conventie.getStudent() != null) {
            html.append("<p><strong>3. Student ").append(conventie.getStudent().getNume() != null ? 
                    conventie.getStudent().getNume() : "")
                .append(" ").append(conventie.getStudent().getPrenume() != null ? 
                    conventie.getStudent().getPrenume() : "").append("</strong>, ");
            
            String cnp = conventie.getStudent().getCnp() != null ? 
                conventie.getStudent().getCnp() : "N/A";
            html.append("CNP ").append(cnp);
            
            String dataNasterii = conventie.getStudent().getDataNasterii() != null ? 
                dateFormat.format(conventie.getStudent().getDataNasterii()) : "N/A";
            html.append(", data nașterii ").append(dataNasterii);
            
            String loculNasterii = conventie.getStudent().getLoculNasterii() != null ? 
                conventie.getStudent().getLoculNasterii() : "N/A";
            String cetatenie = conventie.getStudent().getCetatenie() != null ? 
                conventie.getStudent().getCetatenie() : "N/A";
            html.append(", locul nașterii ").append(loculNasterii)
                .append(", cetățenie ").append(cetatenie);
            
            String serieCi = conventie.getStudent().getSerieCi() != null ? 
                conventie.getStudent().getSerieCi() : "N/A";
            String numarCi = conventie.getStudent().getNumarCi() != null ? 
                conventie.getStudent().getNumarCi() : "N/A";
            html.append(", CI seria ").append(serieCi)
                .append(" nr. ").append(numarCi);
            
            String adresa = conventie.getStudent().getAdresa() != null ? 
                conventie.getStudent().getAdresa() : "N/A";
            String anUniversitar = conventie.getStudent().getAnUniversitar() != null ? 
                conventie.getStudent().getAnUniversitar() : "N/A";
            html.append(", adresa ").append(adresa)
                .append(", înscris în anul universitar ").append(anUniversitar);
            
            String facultate = conventie.getStudent().getFacultate() != null ? 
                conventie.getStudent().getFacultate() : "N/A";
            String specializare = conventie.getStudent().getSpecializare() != null ? 
                conventie.getStudent().getSpecializare() : "N/A";
            html.append(", Universitatea Politehnica Timișoara, ")
                .append("facultatea ").append(facultate)
                .append(", specializarea ").append(specializare);
            
            Integer anDeStudiu = conventie.getStudent().getAnDeStudiu();
            String emailStudent = conventie.getStudent().getEmail() != null ? 
                conventie.getStudent().getEmail() : "N/A";
            String telefonStudent = conventie.getStudent().getTelefon() != null ? 
                conventie.getStudent().getTelefon() : "N/A";
            html.append(", anul de studiu ").append(anDeStudiu != null ? anDeStudiu : "N/A")
                .append(", email ").append(emailStudent)
                .append(", telefon ").append(telefonStudent)
                .append(", denumit în continuare <strong>practicant</strong></p></div>");
        } else {
            html.append("<p><strong>3. [Student nedefinit]</strong></p></div>");
        }
        
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
            .append("de la ").append(formatDate(conventie.getDataInceput()))
            .append(" până la ").append(formatDate(conventie.getDataSfarsit())).append("</p>");

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

        // Art. 12 - Condiții facultative
        html.append("<h3>Art. 12. Condiții facultative de desfășurare a stagiului de pregătire practică</h3>")
            .append("<p>(1) Îndemnizație, gratificări sau prime acordate practicantului:</p>")
            .append("<p>").append(conventie.getIndemnizatii() != null && !conventie.getIndemnizatii().isEmpty() ?
                conventie.getIndemnizatii() : "Nu este cazul").append("</p>")
            .append("<p>(2) Avantaje eventuale:</p>")
            .append("<p>").append(conventie.getAvantaje() != null && !conventie.getAvantaje().isEmpty() ?
                conventie.getAvantaje() : "Nu este cazul").append("</p>")
            .append("<p>(3) Alte precizări:</p>")
            .append("<p>").append(conventie.getAltePrecizari() != null && !conventie.getAltePrecizari().isEmpty() ?
                conventie.getAltePrecizari() : "Nu este cazul").append("</p>");

        // Articolul 13 și data întocmirii
        String dataIntocmirii = conventie.getDataIntocmirii() != null ? 
            dateFormat.format(conventie.getDataIntocmirii()) : "N/A";
        html.append("<h3>Art. 13. Prevederi finale</h3>")
            .append("<p>Această convenție-cadru s-a încheiat în trei exemplare la data: ")
            .append(dataIntocmirii).append("</p>");

        // Tabel semnături - SIMPLU cu semnături incluse
        html.append("<table class='signature-table'>")
            .append("<tr>")
            .append("<th>Universitatea Politehnica Timișoara<br>Rector</th>")
            .append("<th>").append(conventie.getCompanie() != null ? conventie.getCompanie().getNume() : "N/A").append("<br>")
            .append(conventie.getCompanie() != null ? conventie.getCompanie().getReprezentant() : "N/A").append("</th>")
            .append("<th>Student<br>");
        
        // Nume student pentru semnătură
        if (conventie.getStudent() != null) {
            html.append(conventie.getStudent().getNume() != null ? conventie.getStudent().getNume() : "")
                .append(" ")
                .append(conventie.getStudent().getPrenume() != null ? conventie.getStudent().getPrenume() : "");
        } else {
            html.append("N/A");
        }
        
        html.append("</th>")
            .append("</tr>")
            .append("<tr>")
            .append("<td>");

        // Pentru data și semnătura rectorului
        html.append("Conf. univ. dr. ing. Florin DRĂGAN<br><br>")
            .append("Semnătura: ____________<br>")
            .append("Data: ____________");

        html.append("</td>")
            .append("<td>");

        // Pentru partener - cu semnătură dacă există
        if (conventie.getStatus() == ConventieStatus.APROBATA_PARTENER || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_TUTORE || 
            conventie.getStatus() == ConventieStatus.APROBATA_TUTORE || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRODECAN || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR ||
            conventie.getStatus() == ConventieStatus.APROBATA) {
            
            html.append(conventie.getCompanie() != null ? conventie.getCompanie().getReprezentant() : "N/A")
                .append("<br><br>Semnătura: ");
            
            // Verificăm dacă partenerul actual sau orice partener din companie are semnătură
            if (partner != null && partner.getSemnatura() != null) {
                String base64Signature = Base64.getEncoder().encodeToString(partner.getSemnatura());
                html.append("<img src='data:image/png;base64,").append(base64Signature)
                    .append("' style='max-width:150px; max-height:70px;'><br>");
            } else {
                // Căutăm orice partener din companie cu semnătură
                try {
                    List<Partner> partners = partnerRepository.findByCompanieId(conventie.getCompanie().getId());
                    Partner partnerCuSemnatura = null;
                    
                    if (partners != null && !partners.isEmpty()) {
                        for (Partner p : partners) {
                            if (p.getSemnatura() != null) {
                                partnerCuSemnatura = p;
                                break;
                            }
                        }
                    }
                    
                    if (partnerCuSemnatura != null) {
                        String base64Signature = Base64.getEncoder().encodeToString(partnerCuSemnatura.getSemnatura());
                        html.append("<img src='data:image/png;base64,").append(base64Signature)
                            .append("' style='max-width:150px; max-height:70px;'><br>");
                    } else {
                        html.append("[Semnătură electronică]<br>");
                    }
                } catch (Exception e) {
                    html.append("____________<br>");
                }
            }
            
            html.append("Data: ")
                .append(conventie.getDataIntocmirii() != null ? dateFormat.format(conventie.getDataIntocmirii()) : "____________");
        } else {
            html.append(conventie.getCompanie() != null ? conventie.getCompanie().getReprezentant() : "N/A")
                .append("<br><br>Semnătura: ____________<br>Data: ____________");
        }

        html.append("</td>")
            .append("<td>");

        // Pentru student - cu semnătură dacă există
        if (conventie.getStudent() != null) {
            html.append(conventie.getStudent().getNume()).append(" ")
                .append(conventie.getStudent().getPrenume())
                .append("<br><br>Semnătura: ");
            
            // Verificăm dacă studentul are semnătură încărcată
            if (conventie.getStudent().getSemnatura() != null) {
                String base64Signature = Base64.getEncoder().encodeToString(conventie.getStudent().getSemnatura());
                html.append("<img src='data:image/png;base64,").append(base64Signature)
                    .append("' style='max-width:150px; max-height:70px;'><br>");
            } else {
                html.append("____________<br>");
            }
            
            html.append("Data: ____________");
        } else {
            html.append("N/A<br><br>Semnătura: ____________<br>Data: ____________");
        }

        html.append("</td>")
            .append("</tr>")
            .append("</table>");

        // Am luat la cunoștință - secțiunea cu cadru didactic și tutore
        html.append("<p style='margin-top: 20px;'>Am luat la cunoștință,</p>")
            .append("<table class='signature-table'>")
            .append("<tr>")
            .append("<td><strong>Cadru didactic supervizor</strong><br>");
        
        // Cadru didactic
        if (conventie.getCadruDidactic() != null) {
            html.append(conventie.getCadruDidactic().getNume() != null ? conventie.getCadruDidactic().getNume() : "")
                .append(" ")
                .append(conventie.getCadruDidactic().getPrenume() != null ? conventie.getCadruDidactic().getPrenume() : "")
                .append("<br>")
                .append("Funcția: ").append(conventie.getCadruDidactic().getFunctie() != null ? 
                    conventie.getCadruDidactic().getFunctie() : "N/A");
        } else {
            html.append("N/A<br>Funcția: N/A");
        }
        
        html.append("<br><br>")
            .append("Semnătura: ____________<br>")
            .append("Data: ____________</td>")
            .append("<td><strong>Tutore</strong><br>");
        
        // Tutore
        if (conventie.getTutore() != null) {
            html.append(conventie.getTutore().getNume() != null ? conventie.getTutore().getNume() : "")
                .append(" ")
                .append(conventie.getTutore().getPrenume() != null ? conventie.getTutore().getPrenume() : "")
                .append("<br>")
                .append("Funcția: ").append(conventie.getTutore().getFunctie() != null ? 
                    conventie.getTutore().getFunctie() : "N/A");
        } else {
            html.append("N/A<br>Funcția: N/A");
        }
        
        html.append("<br><br>")
            .append("Semnătura: ____________<br>")
            .append("Data: ____________</td>")
            .append("</tr>")
            .append("</table>");

        html.append("</body></html>");
        return html.toString();
    }

    
    
    @GetMapping("/conventie-export-pdf/{id}")
    public ResponseEntity<byte[]> exportConventiePdf(@PathVariable("id") int id, Authentication authentication) throws IOException, DocumentException {
        User user = (User) authentication.getPrincipal();
        Partner partner = partnerRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("Partner not found"));
        
        Conventie conventie = conventieRepository.findById(id);
        
        if (conventie == null || conventie.getCompanie().getId() != partner.getCompanie().getId()) {
            return ResponseEntity.notFound().build();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(document, out);
        document.open();

        // Font pentru diacritice
        BaseFont baseFont = BaseFont.createFont("c:/windows/fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        Font font = new Font(baseFont, 11);
        Font boldFont = new Font(baseFont, 11, Font.BOLD);
        Font titleFont = new Font(baseFont, 14, Font.BOLD);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

        // Header
        Paragraph header = new Paragraph("ANEXA 3", boldFont);
        header.setAlignment(Element.ALIGN_RIGHT);
        header.add(new Chunk("\nNr. _____ / " + dateFormat.format(new java.util.Date())));
        document.add(header);
        document.add(Chunk.NEWLINE);

        // Titlu
        Paragraph title = new Paragraph("CONVENȚIE-CADRU", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        Paragraph subtitle = new Paragraph("privind efectuarea stagiului de practică în cadrul\nprogramelor de studii universitare de licență sau masterat", titleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subtitle);
        document.add(Chunk.NEWLINE);

        // Părți contractante
        document.add(new Paragraph("Prezenta convenție-cadru se încheie între:", font));
        document.add(Chunk.NEWLINE);

        // UPT
        Paragraph upt = new Paragraph();
        upt.add(new Chunk("1. Universitatea Politehnica Timișoara", boldFont));
        upt.add(new Chunk(", reprezentată de Rector, conf. univ. dr. ing. Florin DRĂGAN, cu sediul în TIMIȘOARA, " +
                "Piața Victoriei, Nr. 2, cod 300006, telefon: 0256-403011, email: rector@upt.ro, " +
                "cod unic de înregistrare: 4269282, denumită în continuare ", font));
        upt.add(new Chunk("organizator de practică", boldFont));
        document.add(upt);
        document.add(Chunk.NEWLINE);

        // Companie
        Paragraph comp = new Paragraph();
        comp.add(new Chunk("2. " + conventie.getCompanie().getNume(), boldFont));
        comp.add(new Chunk(", reprezentată de " + conventie.getCompanie().getReprezentant() +
                " în calitate de " + conventie.getCompanie().getCalitate() +
                ", cu sediul în " + conventie.getCompanie().getAdresa() +
                ", telefon " + conventie.getCompanie().getTelefon() +
                ", denumită în continuare ", font));
        comp.add(new Chunk("partener de practică", boldFont));
        document.add(comp);
        document.add(Chunk.NEWLINE);

        // Student
        Paragraph stud = new Paragraph();
        stud.add(new Chunk("3. Student " + conventie.getStudent().getNume() + " " + 
                conventie.getStudent().getPrenume(), boldFont));
        stud.add(new Chunk(", CNP " + conventie.getStudent().getCnp() +
                ", data nașterii " + dateFormat.format(conventie.getStudent().getDataNasterii()) +
                ", locul nașterii " + conventie.getStudent().getLoculNasterii() +
                ", cetățenie " + conventie.getStudent().getCetatenie() +
                ", CI seria " + conventie.getStudent().getSerieCi() +
                " nr. " + conventie.getStudent().getNumarCi() +
                ", adresa " + conventie.getStudent().getAdresa() +
                ", înscris în anul universitar " + conventie.getStudent().getAnUniversitar() +
                ", facultatea " + conventie.getStudent().getFacultate() +
                ", specializarea " + conventie.getStudent().getSpecializare() +
                ", anul de studiu " + conventie.getStudent().getAnDeStudiu() +
                ", email " + conventie.getStudent().getEmail() +
                ", telefon " + conventie.getStudent().getTelefon() +
                ", denumit în continuare ", font));
        stud.add(new Chunk("practicant", boldFont));
        document.add(stud);
        document.add(Chunk.NEWLINE);

        // Adăugă restul articolelor - similar cu codul din ProdecanController sau StudentController
        addArticleTitle(document, "Art. 1. Obiectul convenției-cadru", boldFont);
        addParagraph(document, "(1) Convenția-cadru stabilește modul în care se organizează și se " +
                    "desfășoară stagiul de practică în vederea consolidării cunoștințelor teoretice și " +
                    "formarea abilităților practice, spre a le aplica în concordanță cu specializarea pentru " +
                    "care se instruiește studentul practicant.", font);
        addParagraph(document, "(2) Stagiul de practică este realizat de practicant în vederea dobândirii " +
                    "competențelor profesionale menționate în Portofoliul de practică care este corelat cu fișa disciplinei de practică, " +
                    "parte integrantă a prezentei convenții. " +
                    "Locul desfășurării stagiului de practică este: " + conventie.getLoculDesfasurarii(), font);
        // Adaugă restul articolelor în mod similar
     // Art. 4
        addArticleTitle(document, "Art. 4. Plata și obligațiile sociale", boldFont);
        addParagraph(document, "(1) Stagiul de pregătire practică (se bifează situația corespunzătoare):", font);
        addParagraph(document, "☐ - se efectuează în cadrul unui contract de muncă, cei doi parteneri putând să beneficieze " +
                     "de prevederile Legii nr. 72/2007 privind stimularea încadrării în muncă a elevilor și studenților;", font);
        addParagraph(document, "☐ - nu se efectuează în cadrul unui contract de muncă;", font);
        addParagraph(document, "☐ - se efectuează în cadrul unui proiect finanțat prin Fondul Social European;", font);
        addParagraph(document, "☐ - se efectuează în cadrul proiectului ...", font);
        addParagraph(document, "(2) În cazul angajării ulterioare, perioada stagiului nu va fi considerată ca vechime " +
                "în muncă în situația în care convenția nu se derulează în cadrul unui contract de muncă.", font);
   addParagraph(document, "(3) Practicantul nu poate pretinde un salariu din partea partenerului de practică, cu " +
                "excepția situației în care practicantul are statut de angajat.", font);
   addParagraph(document, "(4) Partenerul de practică poate totuși acorda practicantului o indemnizație, " +
                "gratificare, primă sau avantaje în natură, conform legislației în vigoare.", font);

   // Art. 5
   addArticleTitle(document, "Art. 5. Responsabilitățile practicantului", boldFont);
   addParagraph(document, "(1) Practicantul are obligația, ca pe durata derulării stagiului de practică, să " +
                "respecte programul de lucru stabilit și să execute activitățile specificate de tutore " +
                "în conformitate cu portofoliul de practică, în condițiile respectării cadrului legal cu " +
                "privire la volumul și dificultatea acestora.", font);
   addParagraph(document, "(2) Pe durata stagiului, practicantul respectă regulamentul de ordine interioară al " +
                "partenerului de practică. În cazul nerespectării acestui regulament, conducătorul " +
                "partenerului de practică își rezervă dreptul de a anula convenția-cadru, după ce în " +
                "prealabil a ascultat punctul de vedere al practicantului și al îndrumătorului de " +
                "practică și a înștiințat conducătorul facultății unde practicantul este înmatriculat " +
                "și după primirea confirmării de primire a acestei informații.", font);
   addParagraph(document, "(3) Practicantul are obligația de a respecta normele de securitate și sănătate în " +
                "muncă pe care le-a însușit de la reprezentantul partenerului de practică înainte de " +
                "începerea stagiului de practică.", font);
   addParagraph(document, "(4) Practicantul se angajează să nu folosească, în niciun caz, informațiile la care " +
                "are acces în timpul stagiului despre partenerul de practică sau clienții săi, pentru a " +
                "le comunica unui terț sau pentru a le publica, chiar după terminarea stagiului, decât " +
                "cu acordul respectivului partener de practică.", font);

   // Art. 6
   addArticleTitle(document, "Art. 6. Responsabilitățile partenerului de practică", boldFont);
   addParagraph(document, "(1) Partenerul de practică va stabili un tutore pentru stagiul de practică, " +
                "selectat dintre salariații proprii și ale cărui obligații sunt menționate în portofoliul " +
                "de practică, parte integrantă a convenției-cadru.", font);
   addParagraph(document, "(2) În cazul nerespectării obligațiilor de către practicant, tutorele va contacta " +
                "cadrul didactic supervizor, responsabil de practică, aplicându-se sancțiuni conform " +
                "legilor și regulamentelor în vigoare.", font);
   addParagraph(document, "(3) Înainte de începerea stagiului de practică, partenerul are obligația de a face " +
                "practicantului instructajul cu privire la normele de securitate și sănătate în muncă, " +
                "în conformitate cu legislația în vigoare. Printre responsabilitățile sale, partenerul de practică " +
                "va lua măsurile necesare pentru securitatea și sănătatea în muncă a practicantului, precum " +
                "și pentru comunicarea regulilor de prevenire a riscurilor profesionale.", font);
   addParagraph(document, "(4) Partenerul de practică trebuie să pună la dispoziția practicantului toate " +
                "mijloacele necesare pentru dobândirea competențelor precizate în portofoliul de practică.", font);
   addParagraph(document, "(5) Partenerul de practică are obligația de a asigura practicantului accesul liber la " +
                "serviciul de medicina muncii, pe durata derulării pregătirii practice.", font);

   // Art. 7
   addArticleTitle(document, "Art. 7. Obligațiile organizatorului de practică", boldFont);
   addParagraph(document, "(1) Organizatorul de practică desemnează un cadru didactic supervizor, responsabil " +
                "cu planificarea, organizarea și supravegherea desfășurării pregătirii practice. " +
                "Cadrul didactic supervizor, împreună cu tutorele desemnat de partenerul de practică " +
                "stabilesc tematica de practică și competențele profesionale care fac obiectul stagiului " +
                "de pregătire practică.", font);
   addParagraph(document, "(2) În cazul în care derularea stagiului de pregătire practică nu este conformă cu " +
                "angajamentele luate de către partenerul de practică în cadrul prezentei convenții, " +
                "conducătorul organizatorului de practică poate decide întreruperea stagiului de pregătire " +
                "practică conform convenției-cadru, după informarea prealabilă a conducătorului partenerului " +
                "de practică și după primirea confirmării de primire a acestei informații.", font);

   // Art. 8
   addArticleTitle(document, "Art. 8. Persoane desemnate de organizatorul de practică și partenerul de practică", boldFont);
   addParagraph(document, "(1) Tutorele (persoana care va avea responsabilitatea practicantului din partea partenerului de practică):", font);
   addParagraph(document, "Dl/Dna " + conventie.getTutore().getNume() + " " + conventie.getTutore().getPrenume() +
                "\nFuncția: " + conventie.getTutore().getFunctie() +
                "\nTelefon: " + conventie.getTutore().getTelefon() +
                "\nEmail: " + conventie.getTutore().getEmail(), font);
   addParagraph(document, "(2) Cadrul didactic supervizor, responsabil cu urmărirea derulării stagiului de practică din partea organizatorului de practică:", font);
   addParagraph(document, "Dl/Dna " + conventie.getCadruDidactic().getNume() + " " + conventie.getCadruDidactic().getPrenume() +
                "\nFuncția: " + conventie.getCadruDidactic().getFunctie() +
                "\nTelefon: " + conventie.getCadruDidactic().getTelefon() +
                "\nEmail: " + conventie.getCadruDidactic().getEmail(), font);

   // Art. 9
   addArticleTitle(document, "Art. 9. Evaluarea stagiului de pregătire practică prin credite transferabile", boldFont);
   addParagraph(document, "Numărul de credite transferabile ce vor fi obținute în urma desfășurării stagiului " +
                "de practică este de " + conventie.getNumarCredite() + ".", font);

   // Art. 10
   addArticleTitle(document, "Art. 10. Raportul privind stagiul de pregătire practică", boldFont);
   addParagraph(document, "(1) În timpul derulării stagiului de practică, tutorele împreună cu cadrul " +
                "didactic supervizor vor evalua practicantul în permanență, pe baza unei fișe de " +
                "observație/evaluare. Vor fi evaluate atât nivelul de dobândire a competențelor " +
                "profesionale, cât și comportamentul și modalitatea de integrare a practicantului în " +
                "activitatea partenerului de practică (disciplină, punctualitate, responsabilitate în " +
                "rezolvarea sarcinilor, respectarea regulamentului de ordine interioară al partenerului de practică etc.).", font);
   addParagraph(document, "(2) La finalul stagiului de practică, tutorele elaborează un raport, pe baza " +
                "evaluării nivelului de dobândire a competențelor de către practicant. Rezultatul " +
                "acestei evaluări va sta la baza notării practicantului de către cadrul didactic " +
                "supervizor.", font);
   addParagraph(document, "(3) Periodic și după încheierea stagiului de practică, practicantul va prezenta un caiet de practică care va cuprinde:", font);
   addParagraph(document, "• denumirea modulului de pregătire;\n" +
                "• competențe exersate;\n" +
                "• activități desfășurate pe perioada stagiului de practică;\n" +
                "• observații personale privitoare la activitatea depusă.", font);

   // Art. 11
   addArticleTitle(document, "Art. 11. Sănătatea și securitatea în muncă. Protecția socială a practicantului", boldFont);
   addParagraph(document, "(1) Practicantul anexează prezentului contract dovada asigurării medicale valabile " +
                "în perioada și pe teritoriul statului unde se desfășoară stagiul de practică.", font);
   addParagraph(document, "(2) Partenerul de practică are obligația respectării prevederilor legale cu privire " +
                "la sănătatea și securitatea în muncă a practicantului pe durata stagiului de practică.", font);
   addParagraph(document, "(3) Practicantului i se asigură protecție socială conform legislației în vigoare. Ca " +
                "urmare, conform dispozițiilor Legii nr. 346/2002 privind asigurările pentru accidente de " +
                "muncă și boli profesionale, cu modificările și completările ulterioare, practicantul " +
                "beneficiază de legislația privitoare la accidentele de muncă pe toata durata efectuării " +
                "pregătirii practice.", font);
   addParagraph(document, "(4) În cazul unui accident suportat de practicant, fie în cursul lucrului, fie în " +
                "timpul deplasării la lucru, partenerul de practică se angajează să înștiințeze " +
                "asiguratorul cu privire la accidentul care a avut loc.", font);

   // Art. 12
   addArticleTitle(document, "Art. 12. Condiții facultative de desfășurare a stagiului de pregătire practică", boldFont);
   addParagraph(document, "(1) Îndemnizație, gratificări sau prime acordate practicantului:", font);
   addParagraph(document, conventie.getIndemnizatii() != null ? conventie.getIndemnizatii() : "Nu este cazul", font);
   addParagraph(document, "(2) Avantaje eventuale:", font);
   addParagraph(document, conventie.getAvantaje() != null ? conventie.getAvantaje() : "Nu este cazul", font);
   addParagraph(document, "(3) Alte precizări:", font);
   addParagraph(document, conventie.getAltePrecizari() != null ? conventie.getAltePrecizari() : "Nu este cazul", font);

   // Art. 13
   addArticleTitle(document, "Art. 13. Prevederi finale", boldFont);
   addParagraph(document, "Prezenta convenție-cadru s-a încheiat în trei exemplare la data: " + 
                dateFormat.format(conventie.getDataIntocmirii()), font);
// La final, adăugăm tabelul de semnături
   document.add(Chunk.NEWLINE);
   PdfPTable table = new PdfPTable(3);
   table.setWidthPercentage(100);
        // Adaugă tabelul de semnături
        addSignatureTable(document, conventie, font, boldFont, authentication);

        document.close();

        String filename = String.format("conventie_%s_%s.pdf", 
            conventie.getStudent().getNume(),
            conventie.getCompanie().getNume());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
    }

    // Metode helper
    private void addArticleTitle(Document document, String title, Font font) throws DocumentException {
        Paragraph paragraph = new Paragraph(title, font);
        paragraph.setSpacingBefore(10f);
        paragraph.setSpacingAfter(10f);
        document.add(paragraph);
    }

    private void addParagraph(Document document, String text, Font font) throws DocumentException {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setIndentationLeft(20f);
        paragraph.setSpacingAfter(5f);
        document.add(paragraph);
    }

 // În PartnerController
    private void addSignatureTable(Document document, Conventie conventie, Font font, Font boldFont, Authentication authentication) throws DocumentException {
        User user = (User) authentication.getPrincipal();
        Partner partner = partnerRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("Partner not found"));
        
        // Primul tabel
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        float[] columnWidths = new float[]{2.5f, 5f, 5f, 5f};
        table.setWidths(columnWidths);

        // Header
        PdfPCell emptyHeader = new PdfPCell(new Paragraph(""));
        PdfPCell uptHeader = new PdfPCell(new Paragraph("Universitatea Politehnica\nTimișoara,\nprin Rector", boldFont));
        PdfPCell partenerHeader = new PdfPCell(new Paragraph("Partener de practică,\nprin Reprezentant", boldFont));
        PdfPCell practicantHeader = new PdfPCell(new Paragraph("Practicant", boldFont));

        uptHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        partenerHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        practicantHeader.setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(emptyHeader);
        table.addCell(uptHeader);
        table.addCell(partenerHeader);
        table.addCell(practicantHeader);

        // Nume și prenume
        PdfPCell numeLabel = new PdfPCell(new Paragraph("Nume și prenume", boldFont));
        numeLabel.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell numeUPT = new PdfPCell(new Paragraph("Conf. univ. dr. ing.\nFlorin DRĂGAN", font));
        PdfPCell numePartener = new PdfPCell(new Paragraph(conventie.getCompanie().getReprezentant(), font));
        PdfPCell numePracticant = new PdfPCell(new Paragraph(conventie.getStudent().getNumeComplet(), font));

        numeUPT.setHorizontalAlignment(Element.ALIGN_CENTER);
        numePartener.setHorizontalAlignment(Element.ALIGN_CENTER);
        numePracticant.setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(numeLabel);
        table.addCell(numeUPT);
        table.addCell(numePartener);
        table.addCell(numePracticant);

        // Data
        PdfPCell dataLabel = new PdfPCell(new Paragraph("Data", boldFont));
        dataLabel.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell dataUPT = new PdfPCell(new Paragraph(".....", font));
        
        PdfPCell dataPartener = new PdfPCell();
        if (conventie.getStatus() == ConventieStatus.APROBATA_PARTENER || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_TUTORE || 
            conventie.getStatus() == ConventieStatus.APROBATA_TUTORE || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRODECAN || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR ||
            conventie.getStatus() == ConventieStatus.APROBATA) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            dataPartener.addElement(new Paragraph(dateFormat.format(conventie.getDataIntocmirii()), font));
        } else {
            dataPartener.addElement(new Paragraph(".....", font));
        }
        
        // Data pentru student
        PdfPCell dataPracticant = new PdfPCell();
        if (conventie.getStatus() != ConventieStatus.NETRIMIS) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            dataPracticant.addElement(new Paragraph(dateFormat.format(conventie.getDataIntocmirii()), font));
        } else {
            dataPracticant.addElement(new Paragraph(".....", font));
        }

        dataUPT.setHorizontalAlignment(Element.ALIGN_CENTER);
        dataPartener.setHorizontalAlignment(Element.ALIGN_CENTER);
        dataPracticant.setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(dataLabel);
        table.addCell(dataUPT);
        table.addCell(dataPartener);
        table.addCell(dataPracticant);

        // Semnătura
        PdfPCell semnLabel = new PdfPCell(new Paragraph("Semnătura", boldFont));
        semnLabel.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell semnUPT = new PdfPCell(new Paragraph(".....", font));
        
        // Semnătura partenerului
     // Semnătura partenerului - actualizată pentru noile statusuri
        PdfPCell semnPartener = new PdfPCell();
        if (conventie.getStatus() == ConventieStatus.APROBATA_PARTENER || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_TUTORE || 
            conventie.getStatus() == ConventieStatus.APROBATA_TUTORE || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRODECAN || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR ||
            conventie.getStatus() == ConventieStatus.APROBATA) {
            
            try {
                List<Partner> partners = partnerRepository.findByCompanieId(conventie.getCompanie().getId());
                Partner partner2 = null;
                
                if (partners != null && !partners.isEmpty()) {
                    for (Partner p : partners) {
                        if (p.getSemnatura() != null) {
                            partner2 = p;
                            break;
                        }
                    }
                }
                
                if (partner2 != null && partner.getSemnatura() != null) {
                    Image signature = Image.getInstance(partner2.getSemnatura());
                    signature.scaleToFit(100, 50);
                    signature.setAlignment(Element.ALIGN_CENTER);
                    semnPartener.addElement(signature);
                } else {
                    semnPartener.addElement(new Paragraph("[Semnătură electronică]", font));
                }
            } catch (Exception e) {
                e.printStackTrace();
                semnPartener.addElement(new Paragraph(".....", font));
            }
        } else {
            semnPartener.addElement(new Paragraph(".....", font));
        }
        
        // Semnătura studentului
        PdfPCell semnPracticant = new PdfPCell();
        if (conventie.getStudent().getSemnatura() != null) {
            try {
                Image signature = Image.getInstance(conventie.getStudent().getSemnatura());
                signature.scaleToFit(100, 50);
                signature.setAlignment(Element.ALIGN_CENTER);
                semnPracticant.addElement(signature);
            } catch (Exception e) {
                e.printStackTrace();
                semnPracticant.addElement(new Paragraph(".....", font));
            }
        } else {
            semnPracticant.addElement(new Paragraph(".....", font));
        }

        semnUPT.setHorizontalAlignment(Element.ALIGN_CENTER);
        semnPartener.setHorizontalAlignment(Element.ALIGN_CENTER);
        semnPracticant.setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(semnLabel);
        table.addCell(semnUPT);
        table.addCell(semnPartener);
        table.addCell(semnPracticant);

        document.add(table);

        // Am luat la cunoștință
        Paragraph amLuat = new Paragraph("Am luat la cunoștință,", font);
        amLuat.setSpacingBefore(20);
        amLuat.setSpacingAfter(20);
        document.add(amLuat);
        
        document.add(Chunk.NEWLINE);

        // Al doilea tabel - Cadru didactic și Tutore 
        // Notă: Chiar dacă e Partner Controller, afișăm structura completă a documentului
        PdfPTable secondTable = new PdfPTable(3);
        secondTable.setWidthPercentage(100);
        float[] secondWidths = new float[]{2.5f, 5f, 5f};
        secondTable.setWidths(secondWidths);

        // Header al doilea tabel
        PdfPCell emptyHeader2 = new PdfPCell(new Paragraph(""));
        PdfPCell supervizorHeader = new PdfPCell(new Paragraph("Cadru didactic supervizor", boldFont));
        PdfPCell tutoreHeader = new PdfPCell(new Paragraph("Tutore", boldFont));

        supervizorHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        tutoreHeader.setHorizontalAlignment(Element.ALIGN_CENTER);

        secondTable.addCell(emptyHeader2);
        secondTable.addCell(supervizorHeader);
        secondTable.addCell(tutoreHeader);

        // Nume și prenume în al doilea tabel
        PdfPCell numeLabel2 = new PdfPCell(new Paragraph("Nume și prenume", boldFont));
        numeLabel2.setHorizontalAlignment(Element.ALIGN_LEFT);
        PdfPCell numeCadruDidactic = new PdfPCell(new Paragraph(conventie.getCadruDidactic().getNumeComplet(), font));
        PdfPCell numeTutore = new PdfPCell(new Paragraph(conventie.getTutore().getNume() + " " + conventie.getTutore().getPrenume(), font));

        numeCadruDidactic.setHorizontalAlignment(Element.ALIGN_CENTER);
        numeTutore.setHorizontalAlignment(Element.ALIGN_CENTER);

        secondTable.addCell(numeLabel2);
        secondTable.addCell(numeCadruDidactic);
        secondTable.addCell(numeTutore);

        // Funcția în al doilea tabel
        PdfPCell functieLabel = new PdfPCell(new Paragraph("Funcția", boldFont));
        functieLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
        PdfPCell functieCadruDidactic = new PdfPCell(new Paragraph(conventie.getCadruDidactic().getFunctie(), font));
        PdfPCell functieTutore = new PdfPCell(new Paragraph(conventie.getTutore().getFunctie(), font));

        functieCadruDidactic.setHorizontalAlignment(Element.ALIGN_CENTER);
        functieTutore.setHorizontalAlignment(Element.ALIGN_CENTER);

        secondTable.addCell(functieLabel);
        secondTable.addCell(functieCadruDidactic);
        secondTable.addCell(functieTutore);

        // Data în al doilea tabel
        PdfPCell dataLabel2 = new PdfPCell(new Paragraph("Data", boldFont));
        dataLabel2.setHorizontalAlignment(Element.ALIGN_LEFT);
        PdfPCell dataCadruDidactic = new PdfPCell(new Paragraph(".....", font));
        PdfPCell dataTutore = new PdfPCell(new Paragraph(".....", font));

        dataCadruDidactic.setHorizontalAlignment(Element.ALIGN_CENTER);
        dataTutore.setHorizontalAlignment(Element.ALIGN_CENTER);

        secondTable.addCell(dataLabel2);
        secondTable.addCell(dataCadruDidactic);
        secondTable.addCell(dataTutore);

        // Semnătura în al doilea tabel
        PdfPCell semnLabel2 = new PdfPCell(new Paragraph("Semnătura", boldFont));
        semnLabel2.setHorizontalAlignment(Element.ALIGN_LEFT);
        PdfPCell semnCadruDidactic = new PdfPCell(new Paragraph(".....", font));
        PdfPCell semnTutore = new PdfPCell(new Paragraph(".....", font));

        semnCadruDidactic.setHorizontalAlignment(Element.ALIGN_CENTER);
        semnTutore.setHorizontalAlignment(Element.ALIGN_CENTER);

        secondTable.addCell(semnLabel2);
        secondTable.addCell(semnCadruDidactic);
        secondTable.addCell(semnTutore);

        document.add(secondTable);
    }
    // Export Word
    @GetMapping("/conventie-export-word/{id}")
    public ResponseEntity<byte[]> exportConventieWord(@PathVariable("id") int id, Authentication authentication) throws IOException {
        User user = (User) authentication.getPrincipal();
        Partner partner = partnerRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("Partner not found"));
        
        Conventie conventie = conventieRepository.findById(id);
        
        if (conventie == null || conventie.getCompanie().getId() != partner.getCompanie().getId()) {
            return ResponseEntity.notFound().build();
        }

        XWPFDocument document = generateWordDocument(conventie, authentication);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.write(out);
        document.close();

        String filename = String.format("conventie_%s_%s.docx", 
            conventie.getStudent().getNume(),
            conventie.getCompanie().getNume());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
    }

    
 // În PartnerController.java - Înlocuiește metoda addSignaturesTableWord cu aceasta:

 // În PartnerController.java - Înlocuiește metoda addSignaturesTableWord cu aceasta:

    private void addSignaturesTableWord(XWPFDocument document, Conventie conventie, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Partner partner = partnerRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new RuntimeException("Partner not found"));
        
        // Adăugăm textul despre întocmire
        XWPFParagraph datePara = document.createParagraph();
        XWPFRun dateRun = datePara.createRun();
        dateRun.setText("Întocmit în trei exemplare la data: " + formatDate(conventie.getDataIntocmirii()) + ".");
        dateRun.addBreak();
        dateRun.addBreak();

        // Primul tabel pentru semnături principale
        XWPFTable mainTable = document.createTable(4, 4);
        
        // Prima linie - header cu bold
        XWPFTableRow headerRow = mainTable.getRow(0);
        headerRow.getCell(0).setText("");
        setCellTextBold(headerRow.getCell(1), "Universitatea Politehnica\nTimișoara,\nprin Rector");
        setCellTextBold(headerRow.getCell(2), "Partener de practică,\nprin Reprezentant");
        setCellTextBold(headerRow.getCell(3), "Practicant");

        // A doua linie - Nume și prenume
        XWPFTableRow nameRow = mainTable.getRow(1);
        setCellTextBold(nameRow.getCell(0), "Nume și prenume");
        setCellText(nameRow.getCell(1), "Conf. univ. dr. ing.\nFlorin DRĂGAN");
        setCellText(nameRow.getCell(2), conventie.getCompanie().getReprezentant());
        setCellText(nameRow.getCell(3), conventie.getStudent().getNumeComplet());

        // A treia linie - Data
        XWPFTableRow dateRow = mainTable.getRow(2);
        setCellTextBold(dateRow.getCell(0), "Data");
        setCellText(dateRow.getCell(1), ".....");
        
        // Data pentru partener
        if (conventie.getStatus() == ConventieStatus.APROBATA_PARTENER || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_TUTORE || 
            conventie.getStatus() == ConventieStatus.APROBATA_TUTORE ||
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRODECAN || 
            conventie.getStatus() == ConventieStatus.APROBATA) {
            setCellText(dateRow.getCell(2), formatDate(conventie.getDataIntocmirii()));
        } else {
            setCellText(dateRow.getCell(2), ".....");
        }
        
        // Data pentru student
        if (conventie.getStatus() != ConventieStatus.NETRIMIS) {
            setCellText(dateRow.getCell(3), formatDate(conventie.getDataIntocmirii()));
        } else {
            setCellText(dateRow.getCell(3), ".....");
        }

        // A patra linie - Semnătura
        XWPFTableRow signRow = mainTable.getRow(3);
        setCellTextBold(signRow.getCell(0), "Semnătura");
        setCellText(signRow.getCell(1), ".....");
        
        // Semnătura partenerului
        XWPFTableCell partnerCell = signRow.getCell(2);
        if (conventie.getStatus() == ConventieStatus.APROBATA_PARTENER || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_TUTORE || 
            conventie.getStatus() == ConventieStatus.APROBATA_TUTORE ||
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRODECAN || 
            conventie.getStatus() == ConventieStatus.APROBATA) {
            
            XWPFParagraph partnerPara = partnerCell.getParagraphs().get(0);
            partnerPara.setAlignment(ParagraphAlignment.CENTER);
            partnerPara.setSpacingBefore(400);
            XWPFRun partnerRun = partnerPara.createRun();
            
            if (partner.getSemnatura() != null) {
                try {
                    partnerRun.addPicture(
                        new ByteArrayInputStream(partner.getSemnatura()),
                        XWPFDocument.PICTURE_TYPE_PNG,
                        "signature.png",
                        Units.toEMU(100),
                        Units.toEMU(50)
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    partnerRun.setText("[Semnătură electronică]");
                }
            } else {
                partnerRun.setText("[Semnătură electronică]");
            }
        } else if (conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PARTENER) {
            XWPFParagraph partnerPara = partnerCell.getParagraphs().get(0);
            partnerPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun partnerRun = partnerPara.createRun();
            partnerRun.setText("(Veți semna electronic)");
        } else {
            setCellText(partnerCell, ".....");
        }
        
        // Semnătura studentului
        XWPFTableCell studentCell = signRow.getCell(3);
        if (conventie.getStudent().getSemnatura() != null) {
            XWPFParagraph studentPara = studentCell.getParagraphs().get(0);
            studentPara.setAlignment(ParagraphAlignment.CENTER);
            studentPara.setSpacingBefore(400);
            XWPFRun studentRun = studentPara.createRun();
            
            try {
                studentRun.addPicture(
                    new ByteArrayInputStream(conventie.getStudent().getSemnatura()),
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "signature.png",
                    Units.toEMU(100),
                    Units.toEMU(50)
                );
            } catch (Exception e) {
                e.printStackTrace();
                studentRun.setText(".....");
            }
        } else {
            setCellText(studentCell, ".....");
        }

        // Am luat la cunoștință
        XWPFParagraph amLuatPara = document.createParagraph();
        XWPFRun amLuatRun = amLuatPara.createRun();
        amLuatRun.setText("Am luat la cunoștință,");
        amLuatRun.addBreak();
        amLuatRun.addBreak();

        // Al doilea tabel pentru supervizori
        XWPFTable supervisorsTable = document.createTable(5, 3);

        // Header
        XWPFTableRow supervisorsHeader = supervisorsTable.getRow(0);
        supervisorsHeader.getCell(0).setText("");
        setCellTextBold(supervisorsHeader.getCell(1), "Cadru didactic supervizor");
        setCellTextBold(supervisorsHeader.getCell(2), "Tutore");

        // Nume și prenume
        XWPFTableRow nameRow2 = supervisorsTable.getRow(1);
        setCellTextBold(nameRow2.getCell(0), "Nume și prenume");
        setCellText(nameRow2.getCell(1), conventie.getCadruDidactic().getNumeComplet());
        setCellText(nameRow2.getCell(2), conventie.getTutore().getNume() + " " + conventie.getTutore().getPrenume());
        
        // Funcția
        XWPFTableRow funcRow = supervisorsTable.getRow(2);
        setCellTextBold(funcRow.getCell(0), "Funcția");
        setCellText(funcRow.getCell(1), conventie.getCadruDidactic().getFunctie());
        setCellText(funcRow.getCell(2), conventie.getTutore().getFunctie());
        
        // Data
        XWPFTableRow dateRow2 = supervisorsTable.getRow(3);
        setCellTextBold(dateRow2.getCell(0), "Data");
        setCellText(dateRow2.getCell(1), ".....");
        // Data pentru tutore - doar câmpul, fără data efectivă
        setCellText(dateRow2.getCell(2), ".....");
        
        // Semnătura
        XWPFTableRow signRow2 = supervisorsTable.getRow(4);
        setCellTextBold(signRow2.getCell(0), "Semnătura");
        setCellText(signRow2.getCell(1), ".....");
        
        // Semnătura tutorelui - doar câmpul, fără semnătura efectivă
        XWPFTableCell tutoreCell = signRow2.getCell(2);
        setCellText(tutoreCell, ".....");
    }
   

    private void setCellTextBold(XWPFTableCell cell, String text) {
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            run.setText(lines[i]);
            if (i < lines.length - 1) {
                run.addBreak();
            }
        }
    }

    private String formatDate(java.util.Date date) {
        if (date == null) {
            return ".....";
        }
        return new SimpleDateFormat("dd.MM.yyyy").format(date);
    }
    

    private XWPFDocument generateWordDocument(Conventie conventie, Authentication authentication) throws IOException {
        XWPFDocument document = new XWPFDocument();
        
        // Setează marginile documentului (1440 twips = 1 inch)
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        CTPageMar pageMar = sectPr.addNewPgMar();
        pageMar.setLeft(BigInteger.valueOf(1440));
        pageMar.setRight(BigInteger.valueOf(1440));
        pageMar.setTop(BigInteger.valueOf(1440));
        pageMar.setBottom(BigInteger.valueOf(1440));

        addHeader(document);
        addTitle(document);
        addParties(document, conventie);
        addArticles(document, conventie);
        // Modificăm și apelul către addSignatures pentru a include authentication
        addSignaturesTableWord(document, conventie, authentication);
        addAnnex(document, conventie);

        return document;
    }
    
    
    
    private void addHeader(XWPFDocument document) {
        XWPFParagraph headerPara = document.createParagraph();
        headerPara.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun headerRun = headerPara.createRun();
        headerRun.setBold(true);
        headerRun.setText("ANEXA 3");
        headerRun.addBreak();
        headerRun.setText("Nr. _____ / " + new SimpleDateFormat("dd.MM.yyyy").format(new java.util.Date()));
        headerRun.addBreak();
    }

    private void addTitle(XWPFDocument document) {
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(14);
        titleRun.setText("CONVENȚIE-CADRU");
        titleRun.addBreak();
        titleRun.setText("privind efectuarea stagiului de practică în cadrul");
        titleRun.addBreak();
        titleRun.setText("programelor de studii universitare de licență sau masterat");
        titleRun.addBreak();
        titleRun.addBreak();
    }

    private void addParties(XWPFDocument document, Conventie conventie) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

        addParagraph(document, "Prezenta convenție-cadru se încheie între:");

        // UPT
        addParagraph(document, "1. Universitatea Politehnica Timișoara, reprezentată de Rector, " +
                     "conf. univ. dr. ing. Florin DRĂGAN, cu sediul în TIMIȘOARA, Piața Victoriei, Nr. 2, " +
                     "cod 300006, telefon: 0256-403011, email: rector@upt.ro, " +
                     "cod unic de înregistrare: 4269282, denumită în continuare organizator de practică");

        // Companie
        addParagraph(document, "2. " + conventie.getCompanie().getNume() + ", " +
                     "reprezentată de " + conventie.getCompanie().getReprezentant() +
                     " în calitate de " + conventie.getCompanie().getCalitate() +
                     ", cu sediul în " + conventie.getCompanie().getAdresa() +
                     ", telefon " + conventie.getCompanie().getTelefon() +
                     ", denumită în continuare partener de practică");

        // Student
        addParagraph(document, "3. Student " + conventie.getStudent().getNume() + " " + 
                     conventie.getStudent().getPrenume() + ", " +
                     "CNP " + conventie.getStudent().getCnp() +
                     ", data nașterii " + dateFormat.format(conventie.getStudent().getDataNasterii()) +
                     ", locul nașterii " + conventie.getStudent().getLoculNasterii() +
                     ", cetățenie " + conventie.getStudent().getCetatenie() +
                     ", CI seria " + conventie.getStudent().getSerieCi() +
                     " nr. " + conventie.getStudent().getNumarCi() +
                     ", adresa " + conventie.getStudent().getAdresa() +
                     ", înscris în anul universitar " + conventie.getStudent().getAnUniversitar() +
                     ", facultatea " + conventie.getStudent().getFacultate() +
                     ", specializarea " + conventie.getStudent().getSpecializare() +
                     ", anul de studiu " + conventie.getStudent().getAnDeStudiu() +
                     ", email " + conventie.getStudent().getEmail() +
                     ", telefon " + conventie.getStudent().getTelefon() +
                     ", denumit în continuare practicant");
    }

    private void addArticles(XWPFDocument document, Conventie conventie) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

        // Art. 1
        addArticleTitle(document, "Art. 1. Obiectul convenției-cadru");
        addParagraph(document, "(1) Convenția-cadru stabilește modul în care se organizează și se " +
                     "desfășoară stagiul de practică în vederea consolidării cunoștințelor teoretice și " +
                     "formarea abilităților practice, spre a le aplica în concordanță cu specializarea pentru " +
                     "care se instruiește studentul practicant.");
        addParagraph(document, "(2) Stagiul de practică este realizat de practicant în vederea dobândirii " +
                     "competențelor profesionale menționate în Portofoliul de practică care este corelat cu fișa disciplinei de practică, " +
                     "parte integrantă a prezentei convenții. " +
                     "Locul desfășurării stagiului de practică este: " + conventie.getLoculDesfasurarii());
        addParagraph(document, "(3) Modalitățile de derulare și conținutul stagiului de practică sunt descrise în " +
                     "prezenta convenție-cadru și în portofoliul de practică din anexă.");

        // Art. 2
        addArticleTitle(document, "Art. 2. Statutul practicantului");
        addParagraph(document, "Practicantul rămâne, pe toată durata stagiului de pregătire practică, student al " +
                     "Universității Politehnica Timișoara.");

        // Art. 3
        addArticleTitle(document, "Art. 3. Durata și perioada desfășurării stagiului de practică");
        addParagraph(document, "(1) Durata stagiului de practică, precizată în planul de învățământ, este de " +
                     conventie.getDurataInPlanulDeInvatamant() + " [h].");
        addParagraph(document, "(2) Perioada desfășurării stagiului de practică este conformă structurii anului universitar curent " +
                     "de la " + dateFormat.format(conventie.getDataInceput()) +
                     " până la " + dateFormat.format(conventie.getDataSfarsit()));

        addArticleTitle(document, "Art. 4. Plata și obligațiile sociale");
        addParagraph(document, "(1) Stagiul de pregătire practică (se bifează situația corespunzătoare):");
        addParagraph(document, "☐ - se efectuează în cadrul unui contract de muncă, cei doi parteneri putând să beneficieze " +
                     "de prevederile Legii nr. 72/2007 privind stimularea încadrării în muncă a elevilor și studenților;");
        addParagraph(document, "☐ - nu se efectuează în cadrul unui contract de muncă;");
        addParagraph(document, "☐ - se efectuează în cadrul unui proiect finanțat prin Fondul Social European;");
        addParagraph(document, "☐ - se efectuează în cadrul proiectului ...");
        addParagraph(document, "(2) În cazul angajării ulterioare, perioada stagiului nu va fi considerată ca vechime " +
                     "în muncă în situația în care convenția nu se derulează în cadrul unui contract de muncă.");
        addParagraph(document, "(3) Practicantul nu poate pretinde un salariu din partea partenerului de practică, cu " +
                     "excepția situației în care practicantul are statut de angajat.");
        addParagraph(document, "(4) Partenerul de practică poate totuși acorda practicantului o indemnizație, " +
                     "gratificare, primă sau avantaje în natură, conform legislației în vigoare.");

        // Art. 5
        addArticleTitle(document, "Art. 5. Responsabilitățile practicantului");
        addParagraph(document, "(1) Practicantul are obligația, ca pe durata derulării stagiului de practică, să " +
                     "respecte programul de lucru stabilit și să execute activitățile specificate de tutore " +
                     "în conformitate cu portofoliul de practică, în condițiile respectării cadrului legal cu " +
                     "privire la volumul și dificultatea acestora.");
        addParagraph(document, "(2) Pe durata stagiului, practicantul respectă regulamentul de ordine interioară al " +
                     "partenerului de practică. În cazul nerespectării acestui regulament, conducătorul " +
                     "partenerului de practică își rezervă dreptul de a anula convenția-cadru, după ce în " +
                     "prealabil a ascultat punctul de vedere al practicantului și al îndrumătorului de " +
                     "practică și a înștiințat conducătorul facultății unde practicantul este înmatriculat " +
                     "și după primirea confirmării de primire a acestei informații. Această situație conduce la refacerea stagiului de practică și la susținerea unui nou colocviu de evaluare în anul universitar următor.");
        addParagraph(document, "(3) Practicantul are obligația de a respecta normele de securitate și sănătate în " +
                     "muncă pe care le-a însușit de la reprezentantul partenerului de practică înainte de " +
                     "începerea stagiului de practică.");
        addParagraph(document, "(4) Practicantul se angajează să nu folosească, în niciun caz, informațiile la care " +
                     "are acces în timpul stagiului despre partenerul de practică sau clienții săi, pentru a " +
                     "le comunica unui terț sau pentru a le publica, chiar după terminarea stagiului, decât " +
                     "cu acordul respectivului partener de practică.");

        // Art. 6
        addArticleTitle(document, "Art. 6. Responsabilitățile partenerului de practică");
        addParagraph(document, "(1) Partenerul de practică va stabili un tutore pentru stagiul de practică, " +
                     "selectat dintre salariații proprii și ale cărui obligații sunt menționate în portofoliul " +
                     "de practică, parte integrantă a convenției-cadru.");
        addParagraph(document, "(2) În cazul nerespectării obligațiilor de către practicant, tutorele va contacta " +
                     "cadrul didactic supervizor, responsabil de practică, aplicându-se sancțiuni conform " +
                     "legilor și regulamentelor în vigoare.");
        addParagraph(document, "(3) Înainte de începerea stagiului de practică, partenerul are obligația de a face " +
                     "practicantului instructajul cu privire la normele de securitate și sănătate în muncă, " +
                     "pentru fiecare loc distinct de practică, în conformitate cu legislația în vigoare. " +
                     "Printre responsabilitățile sale, partenerul de practică va lua măsurile necesare pentru " +
                     "securitatea și sănătatea în muncă a practicantului, precum și pentru comunicarea " +
                     "regulilor de prevenire a riscurilor profesionale.");
        addParagraph(document, "(4) Partenerul de practică trebuie să pună la dispoziția practicantului toate " +
                     "mijloacele necesare pentru desfășurarea activităților precizate în portofoliul de practică.");
        addParagraph(document, "(5) Partenerul de practică are obligația de a asigura practicantului accesul liber " +
                     "la serviciul de medicina muncii, pe durata derulării pregătirii practice.");
        addParagraph(document, "(6) În urma desfășurării cu succes a stagiului, partenerul de practică va acorda " +
                     "studentului, la cerere, o adeverință constatatoare.");

        // Art. 7
        addArticleTitle(document, "Art. 7. Obligațiile organizatorului de practică");
        addParagraph(document, "(1) Organizatorul de practică desemnează un cadru didactic supervizor, responsabil " +
                     "cu planificarea, organizarea și supravegherea desfășurării pregătirii practice. " +
                     "Cadrul didactic supervizor responsabil de practică, împreună cu tutorele desemnat de " +
                     "partenerul de practică stabilesc tematica de practică și competențele profesionale " +
                     "care fac obiectul stagiului de pregătire practică.");
        addParagraph(document, "(2) În cazul în care derularea stagiului de pregătire practică nu este conformă cu " +
                     "angajamentele luate de către partenerul de practică în cadrul prezentei convenții, " +
                     "conducătorul organizatorului de practică poate decide întreruperea stagiului de " +
                     "pregătire practică conform convenției-cadru, după informarea prealabilă a " +
                     "conducătorului partenerului de practică și după primirea confirmării de primire a " +
                     "acestei informații.");

        // Art. 8
        addArticleTitle(document, "Art. 8. Persoane desemnate de organizatorul de practică și partenerul de practică");
        addParagraph(document, "(1) Tutorele (persoana care va avea responsabilitatea practicantului din partea partenerului de practică):");
        addParagraph(document, "Dl/Dna " + conventie.getTutore().getNume() + " " + conventie.getTutore().getPrenume());
        addParagraph(document, "Funcția: " + conventie.getTutore().getFunctie());
        addParagraph(document, "Telefon: " + conventie.getTutore().getTelefon());
        addParagraph(document, "Email: " + conventie.getTutore().getEmail());
        
        addParagraph(document, "(2) Cadrul didactic supervizor, responsabil cu urmărirea derulării stagiului de practică din partea organizatorului de practică:");
        addParagraph(document, "Dl/Dna " + conventie.getCadruDidactic().getNume() + " " + conventie.getCadruDidactic().getPrenume());
        addParagraph(document, "Funcția: " + conventie.getCadruDidactic().getFunctie());
        addParagraph(document, "Telefon: " + conventie.getCadruDidactic().getTelefon());
        addParagraph(document, "Email: " + conventie.getCadruDidactic().getEmail());

        // Art. 9
        addArticleTitle(document, "Art. 9. Evaluarea stagiului de pregătire practică prin credite transferabile");
        addParagraph(document, "Numărul de credite transferabile ce vor fi obținute în urma desfășurării stagiului " +
                     "de practică este de " + conventie.getNumarCredite() + ".");

        // Art. 10
        addArticleTitle(document, "Art. 10. Raportul privind stagiul de pregătire practică");
        addParagraph(document, "(1) În timpul derulării stagiului de practică, tutorele, împreună cu cadrul " +
                     "didactic supervizor, vor evalua practicantul în permanență. Vor fi monitorizate și " +
                     "evaluate atât nivelul de dobândire a competențelor profesionale, cât și " +
                     "comportamentul și modalitatea de integrare a practicantului în activitatea " +
                     "partenerului de practică (disciplină, punctualitate, responsabilitate în rezolvarea " +
                     "sarcinilor, respectarea regulamentului de ordine interioară al partenerului de practică).");
        addParagraph(document, "(2) La finalul stagiului de practică, tutorele completează atestatul de practică " +
                     "și opțional fișa de evaluare, pe baza evaluării nivelului de dobândire a " +
                     "competențelor de către practicant. Rezultatul acestei evaluări va sta la baza notării " +
                     "practicantului de către cadrul didactic supervizor.");
        addParagraph(document, "La finalul stagiului de practică, studentul elaborează un caiet de practică, " +
                     "însușit și de tutorele din partea partenerului de practică. Atestatul și fișa de " +
                     "evaluare completate de tutore vor sta la baza notării studentului conform " +
                     "Regulamentului cadru de organizare și desfășurare a practicii studenților în UPT.");

        // Art. 11
        addArticleTitle(document, "Art. 11. Sănătatea și securitatea în muncă");
        addParagraph(document, "(1) Practicantul anexează prezentului contract dovada asigurării medicale " +
                     "valabile în perioada și pe teritoriul statului unde se desfășoară stagiul de practică.");
        addParagraph(document, "(2) Partenerul de practică are obligația respectării prevederilor legale cu " +
                     "privire la sănătatea și securitatea în muncă a practicatului pe durata stagiului de practică.");
        addParagraph(document, "(3) Practicantului i se asigură protecție socială conform legislației în vigoare. " +
                     "Ca urmare, conform dispozițiilor Legii nr. 346/2002 privind asigurările pentru " +
                     "accidente de muncă și boli profesionale, cu modificările și completările ulterioare, " +
                     "practicantul beneficiază de legislația privitoare la accidentele de muncă pe toată " +
                     "durata efectuării pregătirii practice.");
        addParagraph(document, "(4) În cazul unui accident suferit de practicant, fie în cursul lucrului, fie în " +
                     "timpul deplasării la lucru, partenerul de practică se angajează să înștiințeze " +
                     "asiguratorul cu privire la accidentul care a avut loc.");

        // Art. 12
        addArticleTitle(document, "Art. 12. Condiții facultative de desfășurare a stagiului de pregătire practică");
        addParagraph(document, "(1) Îndemnizație, gratificări sau prime acordate practicantului:");
        addParagraph(document, conventie.getIndemnizatii() != null ? conventie.getIndemnizatii() : "Nu este cazul");
        addParagraph(document, "(2) Avantaje eventuale (plata transportului de la și la locul desfășurării stagiului de practică, " +
                     "tichete de masă, acces la cantina partenerului de practică etc.):");
        addParagraph(document, conventie.getAvantaje() != null ? conventie.getAvantaje() : "Nu este cazul");
        addParagraph(document, "(3) Alte precizări:");
        addParagraph(document, conventie.getAltePrecizari() != null ? conventie.getAltePrecizari() : "Nu este cazul");

        // Art. 13
        addArticleTitle(document, "Art. 13. Prevederi finale");
        addParagraph(document, "Această convenție-cadru s-a încheiat în trei exemplare la data: " + 
                     dateFormat.format(conventie.getDataIntocmirii()));
    }

    
    private void addSignatures(XWPFDocument document, Conventie conventie, Authentication authentication) {
    	   User user = (User) authentication.getPrincipal();
    	   Partner partner = partnerRepository.findByEmail(user.getEmail())
                   .orElseThrow(() -> new RuntimeException("Partner not found"));
    	   
    	   document.createParagraph().createRun().addBreak();
    	   XWPFTable table = document.createTable(2, 3);
    	   table.setWidth("100%");
    	   
    	   XWPFTableRow headerRow = table.getRow(0);
    	   setCellText(headerRow.getCell(0), "Universitatea Politehnica Timișoara\nRector");
    	   setCellText(headerRow.getCell(1), conventie.getCompanie().getNume());
    	   setCellText(headerRow.getCell(2), "Student");
    	   
    	   XWPFTableRow sigRow = table.getRow(1);

    	   if (conventie.getStatus() == ConventieStatus.APROBATA && partner != null && partner.getSemnatura() != null) {
    	       XWPFParagraph para = sigRow.getCell(0).getParagraphs().get(0);
    	       XWPFRun run = para.createRun();
    	       run.setText("Prof. dr. ing. Florin DRĂGAN\n\n");
    	       
    	       try {
    	           run.addPicture(new ByteArrayInputStream(partner.getSemnatura()),
    	                         XWPFDocument.PICTURE_TYPE_PNG,
    	                         "semnatura.png",
    	                         Units.toEMU(100),
    	                         Units.toEMU(50));
    	       } catch (Exception e) {
    	           e.printStackTrace();
    	       }
    	       
    	       run.addBreak();
    	       run.setText("Data: " + new SimpleDateFormat("dd.MM.yyyy").format(conventie.getDataIntocmirii()));
    	   } else {
    	       setCellText(sigRow.getCell(0), 
    	           "Prof. dr. ing. Florin DRĂGAN\n\nSemnătura: ____________\nData: ____________");
    	   }
    	   
    	   setCellText(sigRow.getCell(1), 
    	       conventie.getCompanie().getReprezentant() + "\n\nSemnătura: ____________\nData: ____________");
    	   setCellText(sigRow.getCell(2), 
    	       conventie.getStudent().getNume() + " " + conventie.getStudent().getPrenume() + 
    	       "\n\nSemnătura: ____________\nData: ____________");
    	}
    

   

    private void addAnnex(XWPFDocument document, Conventie conventie) {
        document.createParagraph().createRun().addBreak();
        
        addArticleTitle(document, "ANEXĂ LA CONVENȚIA-CADRU");
        addArticleTitle(document, "PORTOFOLIU DE PRACTICĂ");
        
        // Adaugă detaliile portofoliului...
    }

    private void addArticleTitle(XWPFDocument document, String title) {
        XWPFParagraph para = document.createParagraph();
        XWPFRun run = para.createRun();
        run.setBold(true);
        run.setText(title);
        run.addBreak();
    }

    private void addParagraph(XWPFDocument document, String text) {
        XWPFParagraph para = document.createParagraph();
        para.setIndentationLeft(720); // 0.5 inch indent
        XWPFRun run = para.createRun();
        run.setText(text);
        run.addBreak();
    }

    private void setCellText(XWPFTableCell cell, String text) {
        XWPFParagraph para = cell.getParagraphs().get(0);
        para.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = para.createRun();
        run.setText(text);
    }
    
    

}