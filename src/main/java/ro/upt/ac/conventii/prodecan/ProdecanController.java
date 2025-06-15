package ro.upt.ac.conventii.prodecan;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.lowagie.text.*;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlToken;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import ro.upt.ac.conventii.conventie.Conventie;
import ro.upt.ac.conventii.conventie.ConventieRepository;
import ro.upt.ac.conventii.conventie.ConventieStatus;
import ro.upt.ac.conventii.partner.Partner;
import ro.upt.ac.conventii.partner.PartnerRepository;
import ro.upt.ac.conventii.partner.PartnerService;
import ro.upt.ac.conventii.security.User;
import ro.upt.ac.conventii.security.UserRepository;
import ro.upt.ac.conventii.service.PasswordGeneratorService;
import ro.upt.ac.conventii.student.Student;
import ro.upt.ac.conventii.student.StudentRepository;
import ro.upt.ac.conventii.tutore.Tutore;
import ro.upt.ac.conventii.tutore.TutoreRepository;
import ro.upt.ac.conventii.utils.ValidationUtils;
import ro.upt.ac.conventii.companie.Companie;
import ro.upt.ac.conventii.companie.CompanieRepository;
import ro.upt.ac.conventii.cadruDidactic.CadruDidactic;
import ro.upt.ac.conventii.cadruDidactic.CadruDidacticDTO;
import ro.upt.ac.conventii.cadruDidactic.CadruDidacticRepository;

import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObjectData;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;

@Controller
@RequestMapping("/prodecan")
public class ProdecanController {

    @Autowired
    private ConventieRepository conventieRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private CompanieRepository companieRepository;
    
    @Autowired
    private PartnerRepository partnerRepository; 
    
    @Autowired
    private PartnerService partnerService;
    
    @Autowired
    private TutoreRepository tutoreRepository; 


    
    @Autowired
    private CadruDidacticRepository cadruDidacticRepository;
    // Adaugă aceste constante la începutul clasei
    private static final int PICTURE_TYPE_PNG = 6;
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 50;


    @Autowired
    private ProdecanRepository prodecanRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordGeneratorService passwordGeneratorService;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // Dashboard endpoint - pagina principală pentru prodecan
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return "redirect:/login";
        }

        User user = (User) authentication.getPrincipal();
        model.addAttribute("user", user);

        try {
            // Statistici pentru dashboard
            long totalStudenti = studentRepository.count();
            long totalCompanii = companieRepository.count();
            long totalCadreDidactice = cadruDidacticRepository.count();
            
            model.addAttribute("totalStudenti", totalStudenti);
            model.addAttribute("totalCompanii", totalCompanii);
            model.addAttribute("totalCadreDidactice", totalCadreDidactice);
            
            // Preluăm convențiile care așteaptă aprobarea prodecanului (ultimele 3)
            Pageable lastThree = PageRequest.of(0, 3);
            List<Conventie> conventiiNesemnate = conventieRepository
                .findTop3ByStatusOrderByDataIntocmiriiDesc(ConventieStatus.IN_ASTEPTARE_PRODECAN, lastThree);
            model.addAttribute("conventiiNesemnate", conventiiNesemnate);

            // Preluăm doar ultimele 5 convenții aprobate final
            Pageable topFive = PageRequest.of(0, 5);
            List<Conventie> conventiiSemnate = conventieRepository
                .findTop5ByStatusOrderByDataIntocmiriiDesc(ConventieStatus.APROBATA, topFive);
            model.addAttribute("conventiiSemnate", conventiiSemnate != null ? conventiiSemnate : new ArrayList<>());
            
        } catch (Exception e) {
            // Log eroarea
            System.err.println("Eroare la încărcarea datelor: " + e.getMessage());
            // Setăm valori default
            model.addAttribute("totalStudenti", 0);
            model.addAttribute("totalCompanii", 0);
            model.addAttribute("totalCadreDidactice", 0);
            model.addAttribute("conventiiNesemnate", new ArrayList<>());
            model.addAttribute("conventiiSemnate", new ArrayList<>());
        }
        
        return "prodecan/dashboard";
    }
    
    
    @GetMapping("/studenti")
    public String studenti(Model model, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        model.addAttribute("user", user);
        
        try {
            List<Student> studenti = studentRepository.findAll();
            model.addAttribute("studenti", studenti != null ? studenti : new ArrayList<>());
        } catch (Exception e) {
            System.err.println("Eroare la încărcarea studenților: " + e.getMessage());
            model.addAttribute("studenti", new ArrayList<>());
        }
        return "prodecan/studenti";
    }
    
    @GetMapping("/management/partners/debug")
    @ResponseBody
    public String debugPartners() {
        StringBuilder debug = new StringBuilder();
        
        try {
            // Verificăm conexiunea la baza de date
            debug.append("=== DEBUG INFO ===\n");
            
            // Numărul total de parteneri
            long count = partnerRepository.count();
            debug.append("Total parteneri în baza de date: ").append(count).append("\n\n");
            
            // Listăm toți partenerii folosind query nativ
            debug.append("Parteneri (query nativ):\n");
            List<Partner> nativePartners = partnerRepository.findAll();
            for (Partner p : nativePartners) {
                debug.append("ID=").append(p.getId())
                     .append(", Nume=").append(p.getNume())
                     .append(", Email=").append(p.getEmail())
                     .append("\n");
            }
            
            debug.append("\nParteneri (JPA findAll):\n");
            List<Partner> partners = partnerRepository.findAll();
            for (Partner p : partners) {
                debug.append("ID=").append(p.getId())
                     .append(", Nume=").append(p.getNume())
                     .append(", Email=").append(p.getEmail())
                     .append(", Companie=").append(p.getCompanie() != null ? p.getCompanie().getNume() : "NULL")
                     .append("\n");
            }
            
        } catch (Exception e) {
            debug.append("EROARE: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }
        
        return debug.toString();
    }
    // Management Studenți
    @GetMapping("/student-create")
    public String afiseazaFormularStudent(Model model) {
        model.addAttribute("student", new Student());
        return "prodecan/student-form";
    }

    @PostMapping("/student-create")
    public String creazaStudent(@ModelAttribute Student student, RedirectAttributes redirectAttributes) {
        try {
            // Salvăm studentul în baza de date
            studentRepository.save(student);
            
            // Generăm parola și creăm cont de utilizator
            String parolaTemporara = student.getNume() + student.getPrenume() + student.getAnDeStudiu();
            User userStudent = new User();
            userStudent.setEmail(student.getEmail());
            userStudent.setNume(student.getNume());
            userStudent.setPrenume(student.getPrenume());
            userStudent.setPassword(passwordEncoder.encode(parolaTemporara));
            userStudent.setRole("ROLE_STUDENT");
            userStudent.setEnabled(true);
            userStudent.setFacultate(student.getFacultate());
            userStudent.setSpecializare(student.getSpecializare());
            userStudent.setFirstLogin(true);  // Setăm explicit doar pentru studenți
            
            userRepository.save(userStudent);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Student creat cu succes!\n" +
                "----------------------------------------\n" +
                "Email: " + student.getEmail() + "\n" +
                "PAROLA TEMPORARĂ: " + parolaTemporara + "\n" +
                "----------------------------------------\n" +
                "IMPORTANT: Salvați această parolă!");
                
            return "redirect:/prodecan/studenti";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Eroare la crearea studentului: " + e.getMessage());
            return "redirect:/prodecan/studenti";
        }
    }
    private String formatDate(java.sql.Date sqlDate) {
        if (sqlDate == null) return ".....";
        return new SimpleDateFormat("dd.MM.yyyy").format(sqlDate);
    }

    @GetMapping("/student-edit/{id}")
    public String showEditStudentForm(@PathVariable int id, Model model, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        model.addAttribute("user", user);
        
        Student student = studentRepository.findById(id);
        if (student != null) {
            model.addAttribute("student", student);
            return "prodecan/student-form";
        }
        return "redirect:/prodecan/studenti";
    }

    @PostMapping("/student-edit/{id}")
    public String updateStudent(@PathVariable int id, @ModelAttribute Student updatedStudent, RedirectAttributes redirectAttributes) {
        try {
            // Căutăm studentul după ID folosind EntityManager pentru a ne asigura că entitatea este atașată sesiunii
            Student existingStudent = studentRepository.findById(id);
            if (existingStudent == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Student negăsit!");
                return "redirect:/prodecan/studenti";
            }

            // Salvăm email-ul original
            String originalEmail = existingStudent.getEmail();

            // Copiem toate proprietățile actualizate, exceptând ID-ul și email-ul
            existingStudent.setNume(updatedStudent.getNume());
            existingStudent.setPrenume(updatedStudent.getPrenume());
            existingStudent.setCnp(updatedStudent.getCnp());
            existingStudent.setDataNasterii(updatedStudent.getDataNasterii());
            existingStudent.setLoculNasterii(updatedStudent.getLoculNasterii());
            existingStudent.setCetatenie(updatedStudent.getCetatenie());
            existingStudent.setSerieCi(updatedStudent.getSerieCi());
            existingStudent.setNumarCi(updatedStudent.getNumarCi());
            existingStudent.setAdresa(updatedStudent.getAdresa());
            existingStudent.setAnUniversitar(updatedStudent.getAnUniversitar());
            existingStudent.setFacultate(updatedStudent.getFacultate());
            existingStudent.setSpecializare(updatedStudent.getSpecializare());
            existingStudent.setAnDeStudiu(updatedStudent.getAnDeStudiu());
            existingStudent.setTelefon(updatedStudent.getTelefon());

            // Salvăm studentul actualizat
            studentRepository.save(existingStudent);

            // Actualizăm și utilizatorul asociat
            User userStudent = userRepository.findByEmail(originalEmail);
            if (userStudent != null) {
                userStudent.setNume(updatedStudent.getNume());
                userStudent.setPrenume(updatedStudent.getPrenume());
                userStudent.setFacultate(updatedStudent.getFacultate());
                userStudent.setSpecializare(updatedStudent.getSpecializare());
                userRepository.save(userStudent);
            }

            redirectAttributes.addFlashAttribute("successMessage", "Student actualizat cu succes!");
            return "redirect:/prodecan/studenti";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Eroare la actualizarea studentului: " + e.getMessage());
            return "redirect:/prodecan/student-edit/" + id;
        }
    }
    

    @PostMapping("/student-reset-password/{id}")
    public String resetStudentPassword(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            Student student = studentRepository.findById(id);
            if (student != null) {
                User userStudent = userRepository.findByEmail(student.getEmail());
                if (userStudent != null) {
                    String newPassword = passwordGeneratorService.generateRandomPassword();
                    userStudent.setPassword(passwordEncoder.encode(newPassword));
                    userRepository.save(userStudent);
                    
                    redirectAttributes.addFlashAttribute("successMessage", 
                        "Parolă resetată cu succes!\n" +
                        "----------------------------------------\n" +
                        "Student: " + student.getNume() + " " + student.getPrenume() + "\n" +
                        "Email: " + student.getEmail() + "\n" +
                        "NOUA PAROLĂ: " + newPassword + "\n" +
                        "----------------------------------------\n" +
                        "IMPORTANT: Salvați această parolă!");
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Eroare la resetarea parolei: " + e.getMessage());
        }
        return "redirect:/prodecan/studenti";
    }

    @PostMapping("/upload-semnatura")
    public String uploadSemnatura(@RequestParam("semnatura") MultipartFile file, 
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            Prodecan prodecan = prodecanRepository.findByEmail(user.getEmail());
            
            if (prodecan == null) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Nu s-a găsit profilul de prodecan asociat.");
                return "redirect:/prodecan/dashboard";
            }

            // Verificăm tipul fișierului
            if (!file.getContentType().startsWith("image/")) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Vă rugăm să încărcați doar fișiere imagine (.jpg, .png).");
                return "redirect:/prodecan/dashboard";
            }

            // Salvăm semnătura
            prodecan.setSemnatura(file.getBytes());
            prodecanRepository.save(prodecan);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Semnătura a fost încărcată cu succes!");
            
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Eroare la încărcarea semnăturii: " + e.getMessage());
        }
        
        return "redirect:/prodecan/dashboard";
    }
    
    
    
    @GetMapping("/companii")
    public String companii(Model model) {
        try {
            List<Companie> companii = companieRepository.findAll();
            model.addAttribute("companii", companii != null ? companii : new ArrayList<>());
        } catch (Exception e) {
            System.err.println("Eroare la încărcarea companiilor: " + e.getMessage());
            model.addAttribute("companii", new ArrayList<>());
        }
        return "prodecan/companii";
    }

    // Adăugare companie nouă - formular
    @GetMapping("/companie-create")
    public String showCreateCompanieForm(Model model) {
        model.addAttribute("companie", new Companie());
        return "prodecan/companie-form";
    }

    @Transactional
    @PostMapping("/companie-create")
    public String createCompanie(@ModelAttribute Companie companie, RedirectAttributes redirectAttributes) {
        try {
            // Validările existente
            if (!ValidationUtils.isValidEmail(companie.getEmail())) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Adresa de email nu este validă!");
                return "redirect:/prodecan/companie-create";
            }
            
            if (!ValidationUtils.isValidNrRegCom(companie.getNrRegCom())) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Numărul de înregistrare trebuie să fie în formatul JXX/NNNN/AAAA (ex: JTM/123/2023)!");
                return "redirect:/prodecan/companie-create";
            }
            
            if (!ValidationUtils.isValidCui(companie.getCui())) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "CUI-ul nu este valid!");
                return "redirect:/prodecan/companie-create";
            }
            
            // Validări pentru noile câmpuri
            if (companie.getNumeReprezentant() == null || companie.getNumeReprezentant().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Numele reprezentantului este obligatoriu!");
                return "redirect:/prodecan/companie-create";
            }
            
            if (companie.getPrenumeReprezentant() == null || companie.getPrenumeReprezentant().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Prenumele reprezentantului este obligatoriu!");
                return "redirect:/prodecan/companie-create";
            }
            
            if (companie.getFunctie() == null || companie.getFunctie().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Funcția reprezentantului este obligatorie!");
                return "redirect:/prodecan/companie-create";
            }
            
            // Salvăm compania
            companieRepository.save(companie);
            
            // Obținem compania cu ID după salvare
            Companie savedCompanie = companieRepository.findById(companie.getId())
                .orElseThrow(() -> new RuntimeException("Compania cu ID " + companie.getId() + " nu a fost găsită"));
            
            // Verificăm dacă compania a fost salvată corect
            if (savedCompanie.getId() == 0) {
                throw new RuntimeException("Compania a fost salvată dar nu are un ID valid!");
            }
            
            // Creăm automat partenerul
            Partner partner = new Partner();
            partner.setCompanie(savedCompanie);
            partner.setNume(companie.getNumeReprezentant());
            partner.setPrenume(companie.getPrenumeReprezentant());
            partner.setFunctie(companie.getFunctie());
            partner.setEmail(companie.getEmail());
            partner.setTelefon(companie.getTelefon());
            
            // Salvăm partenerul
            Partner savedPartner = partnerRepository.save(partner);
            
            // Verifică dacă partenerul a fost salvat corect
            if (savedPartner == null || savedPartner.getId() == 0) {
                throw new RuntimeException("Partenerul nu a putut fi salvat corect!");
            }
            
            // Creăm cont de utilizator cu parola formată din nume+prenume
            String temporaryPassword = companie.getPrenumeReprezentant() + companie.getNumeReprezentant();
            User userPartner = new User();
            userPartner.setEmail(partner.getEmail());
            userPartner.setNume(partner.getNume());
            userPartner.setPrenume(partner.getPrenume());
            userPartner.setPassword(passwordEncoder.encode(temporaryPassword));
            userPartner.setRole("ROLE_PARTNER");
            userPartner.setEnabled(true);
            userPartner.setFirstLogin(true);
            
            userRepository.save(userPartner);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Companie creată cu succes!\n" +
                "A fost creat automat și un cont de partener pentru reprezentant.\n" +
                "----------------------------------------\n" +
                "Reprezentant: " + partner.getPrenume() + " " + partner.getNume() + "\n" +
                "Email: " + partner.getEmail() + "\n" +
                "PAROLA: " + temporaryPassword + "\n" +
                "----------------------------------------\n" +
                "IMPORTANT: Salvați această parolă!");
                    
            return "redirect:/prodecan/companii";
            
        } catch (Exception e) {
            // Verificăm dacă eroarea este cauzată de cheie duplicată
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                if (e.getMessage().contains("email")) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "Există deja o companie cu această adresă de email!");
                } else if (e.getMessage().contains("cui")) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "Există deja o companie cu acest CUI!");
                } else if (e.getMessage().contains("nr_reg_com")) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "Există deja o companie cu acest număr de înregistrare!");
                } else {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "Există deja o înregistrare cu aceste date!");
                }
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Eroare la crearea companiei: " + e.getMessage());
            }
            return "redirect:/prodecan/companie-create";
        }
    }
    
 // Fragment pentru actualizarea ProdecanController
 // Metoda de aprobare a unei convenții ar trebui actualizată astfel:

 // Updated aproba method in ProdecanController
    @PostMapping("/conventie/aproba/{id}")
    public String aprobaConventie(@PathVariable int id, Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = (User) authentication.getPrincipal();
            Prodecan prodecan = prodecanRepository.findByEmail(user.getEmail());
            
            // DEBUG: Verifică semnătura prodecanului
            System.out.println("DEBUG APROBARE: Prodecan: " + (prodecan != null ? prodecan.getNumeComplet() : "NULL"));
            System.out.println("DEBUG APROBARE: Are semnătură: " + (prodecan != null && prodecan.getSemnatura() != null));
            if (prodecan != null && prodecan.getSemnatura() != null) {
                System.out.println("DEBUG APROBARE: Mărime semnătură: " + prodecan.getSemnatura().length + " bytes");
            }
            
            if (prodecan == null || prodecan.getSemnatura() == null) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Nu puteți aproba convenția fără o semnătură încărcată. Vă rugăm să încărcați mai întâi semnătura în panoul de control.");
                return "redirect:/prodecan/conventii";
            }

            Conventie conventie = conventieRepository.findById(id);
            if (conventie == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Convenția nu a fost găsită!");
                return "redirect:/prodecan/conventii";
            }
            
            // Verificăm dacă convenția este în statusul corect pentru aprobare de prodecan
            if (conventie.getStatus() != ConventieStatus.IN_ASTEPTARE_PRODECAN) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Convenția nu este în starea corectă pentru aprobare de prodecan!");
                return "redirect:/prodecan/conventii";
            }

            // Aprobăm convenția și o trimitem la prorector
            conventie.setStatus(ConventieStatus.IN_ASTEPTARE_PRORECTOR);
            conventie.setDataIntocmirii(new java.sql.Date(System.currentTimeMillis()));
            conventieRepository.save(conventie);
            
            System.out.println("DEBUG APROBARE: Convenție aprobată cu status: " + conventie.getStatus());
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Convenția a fost aprobată cu succes și trimisă către prorector pentru aprobarea finală!");
                
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "A apărut o eroare la aprobarea convenției: " + e.getMessage());
        }
        
        return "redirect:/prodecan/conventii";
    } // În ProdecanController
 // Modifică metoda pentru a accepta parametrul Authentication
 // În ProdecanController.java, actualizează metoda addSignatureTable pentru PDF:

 // În ProdecanController.java, actualizează metoda addSignatureTable pentru PDF:

 // Înlocuiește metoda addSignatureTable din ProdecanController cu această versiune actualizată

 // Înlocuiește metoda addSignatureTable din ProdecanController cu această versiune corectată

 // Înlocuiește metoda addSignatureTable din ProdecanController cu această versiune corectată

 // Înlocuiește metoda addSignatureTable din ProdecanController cu această versiune corectată

 // Înlocuiește metoda addSignatureTable din ProdecanController cu această versiune corectată

    private void addSignatureTable(Document document, Conventie conventie, Font font, Font boldFont, Authentication authentication) throws DocumentException {
        User user = (User) authentication.getPrincipal();
        Prodecan prodecan = prodecanRepository.findByEmail(user.getEmail());
        
        // Primul tabel - UPT, Partener, Student
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

        // Data pentru UPT - afișează data DOAR dacă convenția a fost aprobată FINAL (nu doar de prodecan)
        PdfPCell dataUPT = new PdfPCell();
        if (conventie.getStatus() == ConventieStatus.APROBATA) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            dataUPT.addElement(new Paragraph(dateFormat.format(conventie.getDataIntocmirii()), font));
        } else {
            dataUPT.addElement(new Paragraph(".....", font));
        }
        
        // Data pentru partener
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

        // Semnătura pentru UPT - folosește semnătura prodecanului DOAR dacă e aprobată final
        PdfPCell semnUPT = new PdfPCell();
        if (conventie.getStatus() == ConventieStatus.APROBATA && 
            prodecan != null && prodecan.getSemnatura() != null) {
            try {
                Image signature = Image.getInstance(prodecan.getSemnatura());
                signature.scaleToFit(100, 50);
                signature.setAlignment(Element.ALIGN_CENTER);
                semnUPT.addElement(signature);
            } catch (Exception e) {
                semnUPT.addElement(new Paragraph(".....", font));
            }
        } else {
            semnUPT.addElement(new Paragraph(".....", font));
        }
        
        // Semnătura partenerului
        PdfPCell semnPartener = new PdfPCell();
        if (conventie.getStatus() == ConventieStatus.APROBATA_PARTENER || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_TUTORE || 
            conventie.getStatus() == ConventieStatus.APROBATA_TUTORE || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRODECAN || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR ||
            conventie.getStatus() == ConventieStatus.APROBATA) {
            
            try {
                List<Partner> partners = partnerRepository.findByCompanieId(conventie.getCompanie().getId());
                Partner partner = null;
                
                if (partners != null && !partners.isEmpty()) {
                    for (Partner p : partners) {
                        if (p.getSemnatura() != null) {
                            partner = p;
                            break;
                        }
                    }
                }
                
                if (partner != null && partner.getSemnatura() != null) {
                    Image signature = Image.getInstance(partner.getSemnatura());
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

        // "Am luat la cunoștință" section
        Paragraph amLuat = new Paragraph("Am luat la cunoștință,", font);
        amLuat.setSpacingBefore(20);
        amLuat.setSpacingAfter(20);
        document.add(amLuat);
        
        document.add(Chunk.NEWLINE);

        // Al doilea tabel - Cadru didactic și Tutore
        PdfPTable secondTable = new PdfPTable(3);
        secondTable.setWidthPercentage(100);
        float[] secondWidths = new float[]{2.5f, 5f, 5f};
        secondTable.setWidths(secondWidths);

        // Header pentru al doilea tabel
        PdfPCell emptyHeader2 = new PdfPCell(new Paragraph(""));
        PdfPCell cadruDidacticHeader = new PdfPCell(new Paragraph("Cadru didactic supervizor", boldFont));
        PdfPCell tutoreHeader = new PdfPCell(new Paragraph("Tutore", boldFont));

        cadruDidacticHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        tutoreHeader.setHorizontalAlignment(Element.ALIGN_CENTER);

        secondTable.addCell(emptyHeader2);
        secondTable.addCell(cadruDidacticHeader);
        secondTable.addCell(tutoreHeader);

        // Nume și prenume pentru al doilea tabel
        PdfPCell numeLabel2 = new PdfPCell(new Paragraph("Nume și prenume", boldFont));
        numeLabel2.setHorizontalAlignment(Element.ALIGN_LEFT);
        PdfPCell numeCadruDidactic = new PdfPCell(new Paragraph(conventie.getCadruDidactic().getNume() + " " + conventie.getCadruDidactic().getPrenume(), font));
        PdfPCell numeTutore = new PdfPCell(new Paragraph(conventie.getTutore().getNume() + " " + conventie.getTutore().getPrenume(), font));

        numeCadruDidactic.setHorizontalAlignment(Element.ALIGN_CENTER);
        numeTutore.setHorizontalAlignment(Element.ALIGN_CENTER);

        secondTable.addCell(numeLabel2);
        secondTable.addCell(numeCadruDidactic);
        secondTable.addCell(numeTutore);

        // Funcția pentru al doilea tabel
        PdfPCell functieLabel = new PdfPCell(new Paragraph("Funcția", boldFont));
        functieLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
        PdfPCell functieCadruDidactic = new PdfPCell(new Paragraph(conventie.getCadruDidactic().getFunctie(), font));
        PdfPCell functieTutore = new PdfPCell(new Paragraph(conventie.getTutore().getFunctie(), font));

        functieCadruDidactic.setHorizontalAlignment(Element.ALIGN_CENTER);
        functieTutore.setHorizontalAlignment(Element.ALIGN_CENTER);

        secondTable.addCell(functieLabel);
        secondTable.addCell(functieCadruDidactic);
        secondTable.addCell(functieTutore);

        // Data pentru al doilea tabel
        PdfPCell dataLabel2 = new PdfPCell(new Paragraph("Data", boldFont));
        dataLabel2.setHorizontalAlignment(Element.ALIGN_LEFT);
        
        // Data pentru cadrul didactic - afișează data dacă are semnătură proprie SAU dacă prodecanul a semnat (pentru cazul când prodecanul semnează în numele universității)
        PdfPCell dataCadruDidactic = new PdfPCell();
        if (conventie.getCadruDidactic().getSemnatura() != null || 
            ((conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR || 
              conventie.getStatus() == ConventieStatus.APROBATA) && 
             prodecan != null && prodecan.getSemnatura() != null)) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            dataCadruDidactic.addElement(new Paragraph(dateFormat.format(conventie.getDataIntocmirii()), font));
        } else {
            dataCadruDidactic.addElement(new Paragraph(".....", font));
        }
        
        // Data pentru tutore
        PdfPCell dataTutore = new PdfPCell();
        if (conventie.getStatus() == ConventieStatus.APROBATA_TUTORE || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRODECAN || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR ||
            conventie.getStatus() == ConventieStatus.APROBATA) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
            dataTutore.addElement(new Paragraph(dateFormat.format(conventie.getDataIntocmirii()), font));
        } else {
            dataTutore.addElement(new Paragraph(".....", font));
        }

        dataCadruDidactic.setHorizontalAlignment(Element.ALIGN_CENTER);
        dataTutore.setHorizontalAlignment(Element.ALIGN_CENTER);

        secondTable.addCell(dataLabel2);
        secondTable.addCell(dataCadruDidactic);
        secondTable.addCell(dataTutore);

        // Semnătura pentru al doilea tabel
        PdfPCell semnLabel2 = new PdfPCell(new Paragraph("Semnătura", boldFont));
        semnLabel2.setHorizontalAlignment(Element.ALIGN_LEFT);
        
        // Semnătura cadrului didactic - dacă are semnătură proprie o folosește, altfel folosește semnătura prodecanului când convenția e aprobată de prodecan
        PdfPCell semnCadruDidactic = new PdfPCell();
        if (conventie.getCadruDidactic().getSemnatura() != null) {
            try {
                Image signature = Image.getInstance(conventie.getCadruDidactic().getSemnatura());
                signature.scaleToFit(100, 50);
                signature.setAlignment(Element.ALIGN_CENTER);
                semnCadruDidactic.addElement(signature);
            } catch (Exception e) {
                e.printStackTrace();
                semnCadruDidactic.addElement(new Paragraph(".....", font));
            }
        } else if ((conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR || 
                    conventie.getStatus() == ConventieStatus.APROBATA) && 
                   prodecan != null && prodecan.getSemnatura() != null) {
            // Dacă cadrul didactic nu are semnătură, folosește semnătura prodecanului când convenția e aprobată de prodecan
            try {
                Image signature = Image.getInstance(prodecan.getSemnatura());
                signature.scaleToFit(100, 50);
                signature.setAlignment(Element.ALIGN_CENTER);
                semnCadruDidactic.addElement(signature);
            } catch (Exception e) {
                e.printStackTrace();
                semnCadruDidactic.addElement(new Paragraph(".....", font));
            }
        } else {
            semnCadruDidactic.addElement(new Paragraph(".....", font));
        }
        
        // Semnătura tutorelui
        PdfPCell semnTutore = new PdfPCell();
        if (conventie.getStatus() == ConventieStatus.APROBATA_TUTORE || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRODECAN || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR ||
            conventie.getStatus() == ConventieStatus.APROBATA) {
            
            try {
                Optional<Tutore> tutoreOpt = tutoreRepository.findByEmail(conventie.getTutore().getEmail());
                if (tutoreOpt.isPresent() && tutoreOpt.get().getSemnatura() != null) {
                    Image signature = Image.getInstance(tutoreOpt.get().getSemnatura());
                    signature.scaleToFit(100, 50);
                    signature.setAlignment(Element.ALIGN_CENTER);
                    semnTutore.addElement(signature);
                } else {
                    semnTutore.addElement(new Paragraph("[Semnătură electronică]", font));
                }
            } catch (Exception e) {
                e.printStackTrace();
                semnTutore.addElement(new Paragraph(".....", font));
            }
        } else {
            semnTutore.addElement(new Paragraph(".....", font));
        }

        semnCadruDidactic.setHorizontalAlignment(Element.ALIGN_CENTER);
        semnTutore.setHorizontalAlignment(Element.ALIGN_CENTER);

        secondTable.addCell(semnLabel2);
        secondTable.addCell(semnCadruDidactic);
        secondTable.addCell(semnTutore);

        document.add(secondTable);
    }
    private PdfPCell getEmptyCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private void addSecondTableRow(PdfPTable table, String label, String value1, String value2, Font font, Font boldFont) {
        PdfPCell labelCell = new PdfPCell(new Paragraph(label, boldFont));
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell cell1 = new PdfPCell(new Paragraph(value1, font));
        PdfPCell cell2 = new PdfPCell(new Paragraph(value2, font));

        cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell2.setHorizontalAlignment(Element.ALIGN_CENTER);

        table.addCell(labelCell);
        table.addCell(cell1);
        table.addCell(cell2);
    }
    
 // În ProdecanController - metoda addSignaturesTableWord actualizată
 // Înlocuiește metoda addSignaturesTableWord din ProdecanController cu această versiune actualizată

 // Înlocuiește metoda addSignaturesTableWord din ProdecanController cu această versiune corectată

 // Înlocuiește metoda addSignaturesTableWord din ProdecanController cu această versiune corectată

 // Înlocuiește metoda addSignaturesTableWord din ProdecanController cu această versiune corectată

 // Înlocuiește metoda addSignaturesTableWord din ProdecanController cu această versiune corectată

    private void addSignaturesTableWord(XWPFDocument document, Conventie conventie, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Prodecan prodecan = prodecanRepository.findByEmail(user.getEmail());
        
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
        
        // Data pentru prodecan - DOAR dacă convenția a fost aprobată FINAL
        if (conventie.getStatus() == ConventieStatus.APROBATA && conventie.getDataIntocmirii() != null) {
            setCellText(dateRow.getCell(1), formatDate(conventie.getDataIntocmirii()));
        } else {
            setCellText(dateRow.getCell(1), ".....");
        }
        
        // Data pentru partener - dacă a fost aprobată de partener sau mai departe
        if (conventie.getStatus() == ConventieStatus.APROBATA_PARTENER || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_TUTORE || 
            conventie.getStatus() == ConventieStatus.APROBATA_TUTORE || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRODECAN || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR ||
            conventie.getStatus() == ConventieStatus.APROBATA) {
            setCellText(dateRow.getCell(2), formatDate(conventie.getDataIntocmirii()));
        } else {
            setCellText(dateRow.getCell(2), ".....");
        }
        
        // Data pentru student - dacă convenția nu este în starea NETRIMIS
        if (conventie.getStatus() != ConventieStatus.NETRIMIS && conventie.getDataIntocmirii() != null) {
            setCellText(dateRow.getCell(3), formatDate(conventie.getDataIntocmirii()));
        } else {
            setCellText(dateRow.getCell(3), ".....");
        }

        // A patra linie - Semnătura
        XWPFTableRow signRow = mainTable.getRow(3);
        setCellTextBold(signRow.getCell(0), "Semnătura");
        
        // Semnătura prodecanului - DOAR dacă convenția a fost aprobată FINAL
        XWPFTableCell prodecanCell = signRow.getCell(1);
        if (conventie.getStatus() == ConventieStatus.APROBATA && prodecan != null && prodecan.getSemnatura() != null) {
            XWPFParagraph prodecanPara = prodecanCell.getParagraphs().get(0);
            prodecanPara.setAlignment(ParagraphAlignment.CENTER);
            prodecanPara.setSpacingBefore(400);
            XWPFRun prodecanRun = prodecanPara.createRun();
            
            try {
                prodecanRun.addPicture(
                    new ByteArrayInputStream(prodecan.getSemnatura()),
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "signature.png",
                    Units.toEMU(100),
                    Units.toEMU(50)
                );
            } catch (Exception e) {
                e.printStackTrace();
                prodecanRun.setText(".....");
            }
        } else {
            setCellText(prodecanCell, ".....");
        }
        
        // Semnătura partenerului - dacă a fost aprobată de partener sau mai departe
        XWPFTableCell partenerCell = signRow.getCell(2);
        if (conventie.getStatus() == ConventieStatus.APROBATA_PARTENER || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_TUTORE || 
            conventie.getStatus() == ConventieStatus.APROBATA_TUTORE || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRODECAN || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR ||
            conventie.getStatus() == ConventieStatus.APROBATA) {
            
            try {
                // Găsim partenerul după companie
                List<Partner> partners = partnerRepository.findByCompanieId(conventie.getCompanie().getId());
                Partner partner = null;
                
                // Căutăm primul partener cu semnătură
                if (partners != null && !partners.isEmpty()) {
                    for (Partner p : partners) {
                        if (p.getSemnatura() != null) {
                            partner = p;
                            break;
                        }
                    }
                }
                
                if (partner != null && partner.getSemnatura() != null) {
                    XWPFParagraph partenerPara = partenerCell.getParagraphs().get(0);
                    partenerPara.setAlignment(ParagraphAlignment.CENTER);
                    partenerPara.setSpacingBefore(400);
                    XWPFRun partenerRun = partenerPara.createRun();
                    
                    partenerRun.addPicture(
                        new ByteArrayInputStream(partner.getSemnatura()),
                        XWPFDocument.PICTURE_TYPE_PNG,
                        "signature.png",
                        Units.toEMU(100),
                        Units.toEMU(50)
                    );
                } else {
                    setCellText(partenerCell, "[Semnătură electronică]");
                }
            } catch (Exception e) {
                e.printStackTrace();
                setCellText(partenerCell, ".....");
            }
        } else {
            setCellText(partenerCell, ".....");
        }
        
        // Semnătura studentului - dacă convenția nu este în starea NETRIMIS
        XWPFTableCell studentCell = signRow.getCell(3);
        if (conventie.getStatus() != ConventieStatus.NETRIMIS && conventie.getStudent().getSemnatura() != null) {
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

        // Al doilea tabel pentru cadrul didactic și tutore
        XWPFTable secondTable = document.createTable(5, 3);

        // Header cu bold
        XWPFTableRow secondHeaderRow = secondTable.getRow(0);
        secondHeaderRow.getCell(0).setText("");
        setCellTextBold(secondHeaderRow.getCell(1), "Cadru didactic supervizor");
        setCellTextBold(secondHeaderRow.getCell(2), "Tutore");

        // Nume și prenume cu bold pentru etichetă
        XWPFTableRow secondNameRow = secondTable.getRow(1);
        setCellTextBold(secondNameRow.getCell(0), "Nume și prenume");
        setCellText(secondNameRow.getCell(1), conventie.getCadruDidactic().getNumeComplet());
        setCellText(secondNameRow.getCell(2), conventie.getTutore().getNume() + " " + conventie.getTutore().getPrenume());

        // Funcția cu bold pentru etichetă
        XWPFTableRow functionRow = secondTable.getRow(2);
        setCellTextBold(functionRow.getCell(0), "Funcția");
        setCellText(functionRow.getCell(1), conventie.getCadruDidactic().getFunctie());
        setCellText(functionRow.getCell(2), conventie.getTutore().getFunctie());

        // Data cu bold pentru etichetă
        XWPFTableRow secondDateRow = secondTable.getRow(3);
        setCellTextBold(secondDateRow.getCell(0), "Data");
        
        // Data pentru cadrul didactic - afișează data dacă are semnătură proprie SAU dacă prodecanul a semnat
        if (conventie.getCadruDidactic().getSemnatura() != null || 
            ((conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR || 
              conventie.getStatus() == ConventieStatus.APROBATA) && 
             prodecan != null && prodecan.getSemnatura() != null)) {
            setCellText(secondDateRow.getCell(1), formatDate(conventie.getDataIntocmirii()));
        } else {
            setCellText(secondDateRow.getCell(1), ".....");
        }
        
        // Data pentru tutore - dacă a fost aprobată de tutore sau mai departe
        if (conventie.getStatus() == ConventieStatus.APROBATA_TUTORE || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRODECAN || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR ||
            conventie.getStatus() == ConventieStatus.APROBATA) {
            setCellText(secondDateRow.getCell(2), formatDate(conventie.getDataIntocmirii()));
        } else {
            setCellText(secondDateRow.getCell(2), ".....");
        }

        // Semnătura cu bold pentru etichetă
        XWPFTableRow secondSignRow = secondTable.getRow(4);
        setCellTextBold(secondSignRow.getCell(0), "Semnătura");
        
        // Semnătura cadrului didactic - dacă are semnătură proprie o folosește, altfel folosește semnătura prodecanului
        XWPFTableCell cadruDidacticCell = secondSignRow.getCell(1);
        if (conventie.getCadruDidactic().getSemnatura() != null) {
            XWPFParagraph cadruDidacticPara = cadruDidacticCell.getParagraphs().get(0);
            cadruDidacticPara.setAlignment(ParagraphAlignment.CENTER);
            cadruDidacticPara.setSpacingBefore(400);
            XWPFRun cadruDidacticRun = cadruDidacticPara.createRun();
            
            try {
                cadruDidacticRun.addPicture(
                    new ByteArrayInputStream(conventie.getCadruDidactic().getSemnatura()),
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "signature.png",
                    Units.toEMU(100),
                    Units.toEMU(50)
                );
            } catch (Exception e) {
                e.printStackTrace();
                cadruDidacticRun.setText(".....");
            }
        } else if ((conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR || 
                    conventie.getStatus() == ConventieStatus.APROBATA) && 
                   prodecan != null && prodecan.getSemnatura() != null) {
            // Dacă cadrul didactic nu are semnătură, folosește semnătura prodecanului
            XWPFParagraph cadruDidacticPara = cadruDidacticCell.getParagraphs().get(0);
            cadruDidacticPara.setAlignment(ParagraphAlignment.CENTER);
            cadruDidacticPara.setSpacingBefore(400);
            XWPFRun cadruDidacticRun = cadruDidacticPara.createRun();
            
            try {
                cadruDidacticRun.addPicture(
                    new ByteArrayInputStream(prodecan.getSemnatura()),
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "signature.png",
                    Units.toEMU(100),
                    Units.toEMU(50)
                );
            } catch (Exception e) {
                e.printStackTrace();
                cadruDidacticRun.setText(".....");
            }
        } else {
            setCellText(cadruDidacticCell, ".....");
        }
        
        // Semnătura tutorelui
        XWPFTableCell tutoreCell = secondSignRow.getCell(2);
        if (conventie.getStatus() == ConventieStatus.APROBATA_TUTORE || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRODECAN || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR ||
            conventie.getStatus() == ConventieStatus.APROBATA) {
            
            try {
                // Găsim tutorele după email
                Optional<Tutore> tutoreOpt = tutoreRepository.findByEmail(conventie.getTutore().getEmail());
                if (tutoreOpt.isPresent() && tutoreOpt.get().getSemnatura() != null) {
                    XWPFParagraph tutorePara = tutoreCell.getParagraphs().get(0);
                    tutorePara.setAlignment(ParagraphAlignment.CENTER);
                    tutorePara.setSpacingBefore(400);
                    XWPFRun tutoreRun = tutorePara.createRun();
                    
                    tutoreRun.addPicture(
                        new ByteArrayInputStream(tutoreOpt.get().getSemnatura()),
                        XWPFDocument.PICTURE_TYPE_PNG,
                        "signature.png",
                        Units.toEMU(100),
                        Units.toEMU(50)
                    );
                } else {
                    setCellText(tutoreCell, "[Semnătură electronică]");
                }
            } catch (Exception e) {
                e.printStackTrace();
                setCellText(tutoreCell, ".....");
            }
        } else {
            setCellText(tutoreCell, ".....");
        }
    }
    // Metodă helper pentru text bold
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

  

    
    // Editare companie - formular
    @GetMapping("/companie-edit/{id}")
    public String showEditCompanieForm(@PathVariable int id, Model model) {
    	Companie companie = companieRepository.findById(id)
    		    .orElseThrow(() -> new RuntimeException("Compania cu ID " + id + " nu a fost găsită"));

        if (companie != null) {
            model.addAttribute("companie", companie);
            return "prodecan/companie-form";
        }
        // Modificat redirect către pagina prodecanului
        return "redirect:/prodecan/companii";
    }

 // Salvare modificări companie - actualizată pentru noua structură
    @PostMapping("/companie-edit/{id}")
    public String updateCompanie(@PathVariable int id, @ModelAttribute Companie companie, RedirectAttributes redirectAttributes) {
        try {
            // Găsim compania existentă
            Companie existingCompanie = companieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compania cu ID " + id + " nu a fost găsită"));
            
            // Validări pentru noile câmpuri
            if (companie.getNumeReprezentant() == null || companie.getNumeReprezentant().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Numele reprezentantului este obligatoriu!");
                return "redirect:/prodecan/companie-edit/" + id;
            }
            
            if (companie.getPrenumeReprezentant() == null || companie.getPrenumeReprezentant().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Prenumele reprezentantului este obligatoriu!");
                return "redirect:/prodecan/companie-edit/" + id;
            }
            
            if (companie.getFunctie() == null || companie.getFunctie().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Funcția reprezentantului este obligatorie!");
                return "redirect:/prodecan/companie-edit/" + id;
            }
            
            // Salvăm email-ul original pentru a găsi partenerul
            String originalEmail = existingCompanie.getEmail();
            
            // Actualizăm compania
            existingCompanie.setNume(companie.getNume());
            existingCompanie.setNumeReprezentant(companie.getNumeReprezentant());
            existingCompanie.setPrenumeReprezentant(companie.getPrenumeReprezentant());
            existingCompanie.setFunctie(companie.getFunctie());
            existingCompanie.setAdresa(companie.getAdresa());
            existingCompanie.setTelefon(companie.getTelefon());
            existingCompanie.setCui(companie.getCui());
            existingCompanie.setNrRegCom(companie.getNrRegCom());
            
            // Notă: Email-ul nu se poate schimba pentru a menține integritatea cu partenerul
            // existingCompanie.setEmail(companie.getEmail()); // Comentat intenționat
            
            companieRepository.save(existingCompanie);
            
            // Actualizăm și partenerul asociat
            List<Partner> partneriAsociati = partnerRepository.findByCompanie(existingCompanie);
            if (!partneriAsociati.isEmpty()) {
                Partner partner = partneriAsociati.get(0); // Luăm primul partener asociat
                partner.setNume(companie.getNumeReprezentant());
                partner.setPrenume(companie.getPrenumeReprezentant());
                partner.setFunctie(companie.getFunctie());
                partner.setTelefon(companie.getTelefon());
                
                partnerRepository.save(partner);
                
                // Actualizăm și contul de utilizator al partenerului
                User userPartner = userRepository.findByEmail(originalEmail);
                if (userPartner != null) {
                    userPartner.setNume(companie.getNumeReprezentant());
                    userPartner.setPrenume(companie.getPrenumeReprezentant());
                    userRepository.save(userPartner);
                }
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Compania a fost actualizată cu succes! Datele partenerului asociat au fost actualizate automat.");
            
            return "redirect:/prodecan/companii";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Eroare la actualizarea companiei: " + e.getMessage());
            return "redirect:/prodecan/companie-edit/" + id;
        }
    }

 // Ștergere companie - actualizată pentru a șterge și partenerul asociat
    @GetMapping("/companie-delete/{id}")
    public String deleteCompanie(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            Companie companie = companieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Compania cu ID " + id + " nu a fost găsită"));

            if (companie != null) {
                // Verificăm dacă există convenții asociate acestei companii
                List<Conventie> conventiiAsociate = conventieRepository.findByCompanie(companie);
                if (!conventiiAsociate.isEmpty()) {
                    redirectAttributes.addFlashAttribute("errorMessage", 
                        "Nu se poate șterge compania deoarece există " + conventiiAsociate.size() + 
                        " convenții asociate! Ștergeți mai întâi convențiile.");
                    return "redirect:/prodecan/companii";
                }
                
                // Găsim și ștergem partenerii asociați companiei
                List<Partner> partneriAsociati = partnerRepository.findByCompanie(companie);
                for (Partner partner : partneriAsociati) {
                    // Ștergem contul de utilizator al partnerului
                    User userPartner = userRepository.findByEmail(partner.getEmail());
                    if (userPartner != null) {
                        userRepository.delete(userPartner);
                    }
                    // Ștergem partenerul
                    partnerRepository.delete(partner);
                }
                
                // Găsim și ștergem tutorii asociați companiei (dacă există)
                List<Tutore> tutoriAsociati = tutoreRepository.findByCompanie(companie);
                for (Tutore tutore : tutoriAsociati) {
                    // Ștergem contul de utilizator al tutorelui
                    User userTutore = userRepository.findByEmail(tutore.getEmail());
                    if (userTutore != null) {
                        userRepository.delete(userTutore);
                    }
                    // Ștergem tutorele
                    tutoreRepository.delete(tutore);
                }
                
                // În final, ștergem compania
                companieRepository.delete(companie);
                
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Compania a fost ștearsă cu succes împreună cu " + partneriAsociati.size() + 
                    " parteneri asociați și " + tutoriAsociati.size() + " tutori asociați!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Eroare la ștergerea companiei: " + e.getMessage());
        }
        return "redirect:/prodecan/companii";
    }
  

    @PostMapping("/conventie/respinge/{id}")
    public String respingeConventie(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            Conventie conventie = conventieRepository.findById(id);
            if (conventie != null) {
                conventie.setStatus(ConventieStatus.RESPINSA);
                conventie.setDataIntocmirii(new java.sql.Date(System.currentTimeMillis()));
                conventieRepository.save(conventie);
                redirectAttributes.addFlashAttribute("successMessage", "Convenția a fost respinsă.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Eroare la respingerea convenției: " + e.getMessage());
        }
        return "redirect:/prodecan/conventii";
    }
   

    // Actualizează metoda cadreDidactice pentru a folosi DTO
    @GetMapping("/cadre-didactice")
    public String cadreDidactice(Model model, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        model.addAttribute("user", user);
        
        try {
            List<CadruDidactic> cadreDidactice = cadruDidacticRepository.findAll();
            
            // Convertim în DTO-uri pentru a putea accesa Base64 în template
            List<CadruDidacticDTO> cadreDidacticeDTO = cadreDidactice.stream()
                .map(CadruDidacticDTO::new)
                .collect(Collectors.toList());
            
            model.addAttribute("cadreDidactice", cadreDidacticeDTO != null ? cadreDidacticeDTO : new ArrayList<>());
        } catch (Exception e) {
            System.err.println("Eroare la încărcarea cadrelor didactice: " + e.getMessage());
            model.addAttribute("cadreDidactice", new ArrayList<>());
        }
        return "prodecan/cadre-didactice";
    }

    // Formular adăugare cadru didactic
    @GetMapping("/cadru-didactic-create")
    public String showCreateCadruDidacticForm(Model model, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        model.addAttribute("user", user);
        model.addAttribute("cadruDidactic", new CadruDidactic());
        return "prodecan/cadru-didactic-form";
    }

    // Salvare cadru didactic nou
    @PostMapping("/cadru-didactic-create")
    public String createCadruDidactic(@ModelAttribute("cadruDidactic") CadruDidactic cadruDidactic) {
        try {
            cadruDidacticRepository.save(cadruDidactic);
        } catch (Exception e) {
            System.err.println("Eroare la salvarea cadrului didactic: " + e.getMessage());
        }
        return "redirect:/prodecan/cadre-didactice";
    }

    // Formular editare cadru didactic
    @GetMapping("/cadru-didactic-edit/{id}")
    public String showEditCadruDidacticForm(@PathVariable int id, Model model, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        model.addAttribute("user", user);
        
        CadruDidactic cadruDidactic = cadruDidacticRepository.findById(id);
        if (cadruDidactic != null) {
            model.addAttribute("cadruDidactic", cadruDidactic);
            return "prodecan/cadru-didactic-form";
        }
        return "redirect:/prodecan/cadre-didactice";
    }
    
  
    
    @GetMapping("/conventii")
    public String listaConventii(Model model, Authentication authentication) {
        System.out.println("Încercare accesare endpoint conventii"); // logging pentru debug
        
        try {
            User user = (User) authentication.getPrincipal();
            model.addAttribute("user", user);
            
            // Verificăm dacă repository-ul este injectat corect
            if (conventieRepository == null) {
                System.out.println("EROARE: conventieRepository este null!");
                throw new RuntimeException("Repository-ul nu este injectat");
            }

            // Preluăm toate convențiile mai întâi pentru debugging
            List<Conventie> toateConventiile = conventieRepository.findAll();
            System.out.println("Total convenții găsite: " + toateConventiile.size());

            // Convenții care așteaptă aprobarea prodecanului
            List<Conventie> conventiiNesemnate = new ArrayList<>();
            
            // Adăugăm convențiile care sunt în așteptarea prodecanului
            List<Conventie> conventiiAsteptareProdecan = conventieRepository.findByStatus(ConventieStatus.IN_ASTEPTARE_PRODECAN);
            if (conventiiAsteptareProdecan != null && !conventiiAsteptareProdecan.isEmpty()) {
                conventiiNesemnate.addAll(conventiiAsteptareProdecan);
            }
            
            System.out.println("Convenții în așteptare prodecan: " + 
                (conventiiAsteptareProdecan != null ? conventiiAsteptareProdecan.size() : "null"));

            // Convenții care au fost aprobate de prodecan și trimise către prorector
            List<Conventie> conventiiTrimiseProrector = conventieRepository.findByStatus(ConventieStatus.IN_ASTEPTARE_PRORECTOR);
            
            // Convenții semnate complet (aprobate final)
            List<Conventie> conventiiSemnate = conventieRepository.findByStatus(ConventieStatus.APROBATA);
            
            System.out.println("Convenții nesemnate (în așteptare prodecan): " + 
                (conventiiNesemnate != null ? conventiiNesemnate.size() : "null"));
            System.out.println("Convenții trimise la prorector: " + 
                (conventiiTrimiseProrector != null ? conventiiTrimiseProrector.size() : "null"));
            System.out.println("Convenții semnate final: " + 
                (conventiiSemnate != null ? conventiiSemnate.size() : "null"));

            // Inițializăm listele goale dacă e cazul
            conventiiNesemnate = conventiiNesemnate != null ? conventiiNesemnate : new ArrayList<>();
            conventiiTrimiseProrector = conventiiTrimiseProrector != null ? conventiiTrimiseProrector : new ArrayList<>();
            conventiiSemnate = conventiiSemnate != null ? conventiiSemnate : new ArrayList<>();

            model.addAttribute("conventiiNesemnate", conventiiNesemnate);
            model.addAttribute("conventiiTrimiseProrector", conventiiTrimiseProrector);
            model.addAttribute("conventiiSemnate", conventiiSemnate);
            
            return "prodecan/conventii";
            
        } catch (Exception e) {
            System.err.println("Eroare în controller: " + e.getMessage());
            e.printStackTrace(); // Pentru a vedea stack trace-ul complet
            
            // Adăugăm liste goale și mesaj de eroare în model
            model.addAttribute("conventiiNesemnate", new ArrayList<>());
            model.addAttribute("conventiiTrimiseProrector", new ArrayList<>());
            model.addAttribute("conventiiSemnate", new ArrayList<>());
            model.addAttribute("errorMessage", "A apărut o eroare: " + e.getMessage());
            
            return "prodecan/conventii";
        }
    }

    // Salvare modificări cadru didactic
    @PostMapping("/cadru-didactic-edit/{id}")
    public String updateCadruDidactic(@PathVariable int id, @ModelAttribute("cadruDidactic") CadruDidactic cadruDidactic) {
        try {
            cadruDidactic.setId(id);
            cadruDidacticRepository.save(cadruDidactic);
        } catch (Exception e) {
            System.err.println("Eroare la actualizarea cadrului didactic: " + e.getMessage());
        }
        return "redirect:/prodecan/cadre-didactice";
    }

    @GetMapping("/cadru-didactic-delete/{id}")
    public String deleteCadruDidactic(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            // Verificăm dacă există cadrul didactic
            CadruDidactic cadruDidactic = cadruDidacticRepository.findById(id);
            if (cadruDidactic == null) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Cadrul didactic nu a fost găsit în baza de date!");
                return "redirect:/prodecan/cadre-didactice";
            }

            // Verificăm dacă există convenții asociate acestui cadru didactic
            List<Conventie> conventiiAsociate = conventieRepository.findByCadruDidactic(cadruDidactic);
            if (!conventiiAsociate.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Nu se poate șterge cadrul didactic deoarece există convenții asociate!");
                return "redirect:/prodecan/cadre-didactice";
            }

            // Dacă nu există convenții asociate, procedăm cu ștergerea
            cadruDidacticRepository.delete(cadruDidactic);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Cadrul didactic a fost șters cu succes!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "A apărut o eroare la ștergerea cadrului didactic: " + e.getMessage());
        }
        return "redirect:/prodecan/cadre-didactice";
    }
    @GetMapping("/student-delete/{id}")
    public String deleteStudent(@PathVariable int id, RedirectAttributes redirectAttributes) {
        try {
            // Verificăm dacă există studentul
            Student student = studentRepository.findById(id);
            if (student == null) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Studentul nu a fost găsit în baza de date!");
                return "redirect:/prodecan/studenti";
            }

            // Verificăm dacă există convenții asociate acestui student
            List<Conventie> conventiiAsociate = conventieRepository.findByStudent(student);
            if (!conventiiAsociate.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Nu se poate șterge studentul deoarece există convenții asociate!");
                return "redirect:/prodecan/studenti";
            }

            // Găsim și ștergem și contul de utilizator asociat
            User userStudent = userRepository.findByEmail(student.getEmail());
            if (userStudent != null) {
                userRepository.delete(userStudent);
            }

            // Ștergem studentul
            studentRepository.delete(student);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Studentul a fost șters cu succes!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "A apărut o eroare la ștergerea studentului: " + e.getMessage());
        }
        return "redirect:/prodecan/studenti";
    }
    
    
    @PostMapping("/cadru-didactic/upload-semnatura/{id}")
    public String uploadSemnaturaCadruDidactic(@PathVariable int id,
                                              @RequestParam("semnatura") MultipartFile file, 
                                              RedirectAttributes redirectAttributes) {
        try {
            CadruDidactic cadruDidactic = cadruDidacticRepository.findById(id);
            
            if (cadruDidactic == null) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Nu s-a găsit cadrul didactic specificat.");
                return "redirect:/prodecan/cadre-didactice";
            }

            // Verificăm tipul fișierului
            if (!file.getContentType().startsWith("image/")) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Vă rugăm să încărcați doar fișiere imagine (.jpg, .png).");
                return "redirect:/prodecan/cadre-didactice";
            }

            // Verificăm mărimea fișierului (max 2MB)
            if (file.getSize() > 2 * 1024 * 1024) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Fișierul este prea mare. Mărimea maximă permisă este 2MB.");
                return "redirect:/prodecan/cadre-didactice";
            }

            // Salvăm semnătura
            cadruDidactic.setSemnatura(file.getBytes());
            cadruDidacticRepository.save(cadruDidactic);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Semnătura pentru " + cadruDidactic.getNumeComplet() + " a fost încărcată cu succes!");
            
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Eroare la încărcarea semnăturii: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "A apărut o eroare neașteptată: " + e.getMessage());
        }
        
        return "redirect:/prodecan/cadre-didactice";
    }

    // Metoda pentru ștergerea semnăturii cadrului didactic
    @PostMapping("/cadru-didactic/sterge-semnatura/{id}")
    public String stergeSemnaturaCadruDidactic(@PathVariable int id, 
                                              RedirectAttributes redirectAttributes) {
        try {
            CadruDidactic cadruDidactic = cadruDidacticRepository.findById(id);
            
            if (cadruDidactic == null) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Nu s-a găsit cadrul didactic specificat.");
                return "redirect:/prodecan/cadre-didactice";
            }

            // Ștergem semnătura
            cadruDidactic.setSemnatura(null);
            cadruDidacticRepository.save(cadruDidactic);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Semnătura pentru " + cadruDidactic.getNumeComplet() + " a fost ștearsă cu succes!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Eroare la ștergerea semnăturii: " + e.getMessage());
        }
        
        return "redirect:/prodecan/cadre-didactice";
    }
    
    @GetMapping("/conventie-export-word/{id}")
    public ResponseEntity<byte[]> exportConventieWord(@PathVariable("id") int id, Authentication authentication) throws IOException {
        Conventie conventie = conventieRepository.findById(id);
        if (conventie == null) {
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
 // În ProdecanController.java - actualizează metoda de export HTML
    @GetMapping("/conventie-export/{id}")
    public ResponseEntity<String> exportConventie(@PathVariable("id") int id, Authentication authentication) {
        Conventie conventie = conventieRepository.findById(id);
        if (conventie == null) {
            return ResponseEntity.notFound().build();
        }

        String filename = String.format("conventie_%s_%s.html", 
            conventie.getStudent().getNume(),
            conventie.getCompanie().getNume());

        String htmlContent = generateConventieHtml(conventie, authentication);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + filename);
        headers.set(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8");
        // Adăugăm cache control pentru a forța browser-ul să afișeze conținutul
        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");

        return new ResponseEntity<>(htmlContent, headers, HttpStatus.OK);
    }
    
    @GetMapping("/conventie-export-pdf/{id}")
    public ResponseEntity<byte[]> exportConventiePdf(@PathVariable("id") int id, Authentication authentication) throws IOException, DocumentException {
    	Conventie conventie = conventieRepository.findById(id);
        if (conventie == null) {
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

        // Art. 1
        addArticleTitle(document, "Art. 1. Obiectul convenției-cadru", boldFont);
        addParagraph(document, "(1) Convenția-cadru stabilește modul în care se organizează și se " +
                     "desfășoară stagiul de practică în vederea consolidării cunoștințelor teoretice și " +
                     "formarea abilităților practice, spre a le aplica în concordanță cu specializarea pentru " +
                     "care se instruiește studentul practicant.", font);
        addParagraph(document, "(2) Stagiul de practică este realizat de practicant în vederea dobândirii " +
                     "competențelor profesionale menționate în Portofoliul de practică care este corelat cu fișa disciplinei de practică, " +
                     "parte integrantă a prezentei convenții. " +
                     "Locul desfășurării stagiului de practică este: " + conventie.getLoculDesfasurarii(), font);
        addParagraph(document, "(3) Modalitățile de derulare și conținutul stagiului de practică sunt descrise în " +
                     "prezenta convenție-cadru și în portofoliul de practică din anexă.", font);

        // Art. 2
        addArticleTitle(document, "Art. 2. Statutul practicantului", boldFont);
        addParagraph(document, "Practicantul rămâne, pe toată durata stagiului de pregătire practică, student al " +
                     "Universității Politehnica Timișoara.", font);

        // Art. 3
        addArticleTitle(document, "Art. 3. Durata și perioada desfășurării stagiului de practică", boldFont);
        addParagraph(document, "(1) Durata stagiului de practică, precizată în planul de învățământ, este de " +
                     conventie.getDurataInPlanulDeInvatamant() + " [h].", font);
        addParagraph(document, "(2) Perioada desfășurării stagiului de practică este conformă structurii anului universitar curent " +
                     "de la " + dateFormat.format(conventie.getDataInceput()) +
                     " până la " + dateFormat.format(conventie.getDataSfarsit()), font);

        // Continuă cu Art. 4-13...
        
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
    	   Prodecan prodecan = prodecanRepository.findByEmail(user.getEmail());
    	   
    	   document.createParagraph().createRun().addBreak();
    	   XWPFTable table = document.createTable(2, 3);
    	   table.setWidth("100%");
    	   
    	   XWPFTableRow headerRow = table.getRow(0);
    	   setCellText(headerRow.getCell(0), "Universitatea Politehnica Timișoara\nRector");
    	   setCellText(headerRow.getCell(1), conventie.getCompanie().getNume());
    	   setCellText(headerRow.getCell(2), "Student");
    	   
    	   XWPFTableRow sigRow = table.getRow(1);

    	   if (conventie.getStatus() == ConventieStatus.APROBATA && prodecan != null && prodecan.getSemnatura() != null) {
    	       XWPFParagraph para = sigRow.getCell(0).getParagraphs().get(0);
    	       XWPFRun run = para.createRun();
    	       run.setText("Prof. dr. ing. Florin DRĂGAN\n\n");
    	       
    	       try {
    	           run.addPicture(new ByteArrayInputStream(prodecan.getSemnatura()),
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
    
 // În ProdecanController.java - actualizează metoda generateConventieHtml
 // Înlocuiește complet metoda generateConventieHtml din ProdecanController cu această versiune:

    private String generateConventieHtml(Conventie conventie, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Prodecan prodecan = prodecanRepository.findByEmail(user.getEmail());
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
            .append(".signature-image { max-width: 100px; max-height: 50px; }\n")
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
        html.append("<p><strong>2. ").append(conventie.getCompanie().getNume()).append("</strong>, ")
            .append("reprezentată de ").append(conventie.getCompanie().getReprezentant())
            .append(" în calitate de ").append(conventie.getCompanie().getCalitate())
            .append(", cu sediul în ").append(conventie.getCompanie().getAdresa())
            .append(", telefon ").append(conventie.getCompanie().getTelefon())
            .append(", email ").append(conventie.getCompanie().getEmail())
            .append(", cod de înregistrare fiscală: ").append(conventie.getCompanie().getCui())
            .append(", înregistrată la Registrul comerțului cu numărul: ").append(conventie.getCompanie().getNrRegCom())
            .append(", denumită în continuare <strong>partener de practică</strong>,</p>");

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
            .append("de la ").append(formatDate(conventie.getDataInceput()))
            .append(" până la ").append(formatDate(conventie.getDataSfarsit())).append("</p>");

        // Articolele 4-12 (le adaug toate pentru completitudine)
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

        // Articolul 13
        html.append("<h3>Art. 13. Prevederi finale</h3>")
            .append("<p>Această convenție-cadru s-a încheiat în trei exemplare la data: ")
            .append(formatDate(conventie.getDataIntocmirii())).append("</p>");

        // Tabel semnături principale
        html.append("<table class='signature-table'>")
            .append("<tr>")
            .append("<th>Universitatea Politehnica Timișoara<br>Rector</th>")
            .append("<th>").append(conventie.getCompanie().getNume()).append("<br>")
            .append(conventie.getCompanie().getReprezentant()).append("</th>")
            .append("<th>Student<br>")
            .append(conventie.getStudent().getNume()).append(" ")
            .append(conventie.getStudent().getPrenume()).append("</th>")
            .append("</tr>")
            .append("<tr>")
            .append("<td>");

        // Verificăm dacă convenția este aprobată FINAL (nu doar de prodecan) și avem semnătură de prodecan
        if (conventie.getStatus() == ConventieStatus.APROBATA && 
            prodecan != null && prodecan.getSemnatura() != null) {
            // Convertim semnătura în Base64 pentru a o putea afișa în HTML
            String base64Signature = Base64.getEncoder().encodeToString(prodecan.getSemnatura());
            
            html.append("Prof. dr. ing. Florin DRĂGAN<br><br>")
                .append("<img src='data:image/png;base64,")
                .append(base64Signature)
                .append("' class='signature-image'><br>")
                .append("Data: ").append(dateFormat.format(conventie.getDataIntocmirii()));
        } else {
            html.append("Prof. dr. ing. Florin DRĂGAN<br><br>")
                .append("Semnătura: ____________<br>")
                .append("Data: ____________");
        }

        html.append("</td>")
            .append("<td>");
            
        // Semnătura partenerului
        if (conventie.getStatus() == ConventieStatus.APROBATA_PARTENER || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_TUTORE || 
            conventie.getStatus() == ConventieStatus.APROBATA_TUTORE || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRODECAN || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR ||
            conventie.getStatus() == ConventieStatus.APROBATA) {
            
            try {
                List<Partner> partners = partnerRepository.findByCompanieId(conventie.getCompanie().getId());
                Partner partner = null;
                
                if (partners != null && !partners.isEmpty()) {
                    for (Partner p : partners) {
                        if (p.getSemnatura() != null) {
                            partner = p;
                            break;
                        }
                    }
                }
                
                if (partner != null && partner.getSemnatura() != null) {
                    String base64Signature = Base64.getEncoder().encodeToString(partner.getSemnatura());
                    html.append(conventie.getCompanie().getReprezentant())
                        .append("<br><br><img src='data:image/png;base64,")
                        .append(base64Signature)
                        .append("' class='signature-image'><br>Data: ")
                        .append(dateFormat.format(conventie.getDataIntocmirii()));
                } else {
                    html.append(conventie.getCompanie().getReprezentant())
                        .append("<br><br>[Semnătură electronică]<br>Data: ")
                        .append(dateFormat.format(conventie.getDataIntocmirii()));
                }
            } catch (Exception e) {
                html.append(conventie.getCompanie().getReprezentant())
                    .append("<br><br>Semnătura: ____________<br>Data: ____________");
            }
        } else {
            html.append(conventie.getCompanie().getReprezentant())
                .append("<br><br>Semnătura: ____________<br>Data: ____________");
        }
            
        html.append("</td>")
            .append("<td>");
            
        // Semnătura studentului
        if (conventie.getStatus() != ConventieStatus.NETRIMIS && conventie.getStudent().getSemnatura() != null) {
            String base64Signature = Base64.getEncoder().encodeToString(conventie.getStudent().getSemnatura());
            html.append(conventie.getStudent().getNume()).append(" ")
                .append(conventie.getStudent().getPrenume())
                .append("<br><br><img src='data:image/png;base64,")
                .append(base64Signature)
                .append("' class='signature-image'><br>Data: ")
                .append(dateFormat.format(conventie.getDataIntocmirii()));
        } else {
            html.append(conventie.getStudent().getNume()).append(" ")
                .append(conventie.getStudent().getPrenume())
                .append("<br><br>Semnătura: ____________<br>Data: ____________");
        }
            
        html.append("</td>")
            .append("</tr>")
            .append("</table>");

        // Am luat la cunoștință
        html.append("<p class='mt-4'>Am luat la cunoștință,</p>")
            .append("<table class='signature-table'>")
            .append("<tr>")
            .append("<td><strong>Cadru didactic supervizor</strong><br>")
            .append(conventie.getCadruDidactic().getNume()).append(" ")
            .append(conventie.getCadruDidactic().getPrenume()).append("<br>")
            .append("Funcția: ").append(conventie.getCadruDidactic().getFunctie()).append("<br><br>");
            
        // Semnătura cadrului didactic - dacă are semnătură proprie o folosește, altfel folosește semnătura prodecanului
        if (conventie.getCadruDidactic().getSemnatura() != null) {
            String base64Signature = Base64.getEncoder().encodeToString(conventie.getCadruDidactic().getSemnatura());
            html.append("<img src='data:image/png;base64,")
                .append(base64Signature)
                .append("' class='signature-image'><br>")
                .append("Data: ").append(dateFormat.format(conventie.getDataIntocmirii()));
        } else if ((conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR || 
                    conventie.getStatus() == ConventieStatus.APROBATA) && 
                   prodecan != null && prodecan.getSemnatura() != null) {
            // Dacă cadrul didactic nu are semnătură, folosește semnătura prodecanului
            String base64Signature = Base64.getEncoder().encodeToString(prodecan.getSemnatura());
            html.append("<img src='data:image/png;base64,")
                .append(base64Signature)
                .append("' class='signature-image'><br>")
                .append("Data: ").append(dateFormat.format(conventie.getDataIntocmirii()));
        } else {
            html.append("Semnătura: ____________<br>")
                .append("Data: ____________");
        }
            
        html.append("</td>")
            .append("<td><strong>Tutore</strong><br>")
            .append(conventie.getTutore().getNume()).append(" ")
            .append(conventie.getTutore().getPrenume()).append("<br>")
            .append("Funcția: ").append(conventie.getTutore().getFunctie()).append("<br><br>");
            
        // Semnătura tutorelui
        if (conventie.getStatus() == ConventieStatus.APROBATA_TUTORE || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRODECAN || 
            conventie.getStatus() == ConventieStatus.IN_ASTEPTARE_PRORECTOR ||
            conventie.getStatus() == ConventieStatus.APROBATA) {
            
            try {
                Optional<Tutore> tutoreOpt = tutoreRepository.findByEmail(conventie.getTutore().getEmail());
                if (tutoreOpt.isPresent() && tutoreOpt.get().getSemnatura() != null) {
                    String base64Signature = Base64.getEncoder().encodeToString(tutoreOpt.get().getSemnatura());
                    html.append("<img src='data:image/png;base64,")
                        .append(base64Signature)
                        .append("' class='signature-image'><br>")
                        .append("Data: ").append(dateFormat.format(conventie.getDataIntocmirii()));
                } else {
                    html.append("[Semnătură electronică]<br>")
                        .append("Data: ").append(dateFormat.format(conventie.getDataIntocmirii()));
                }
            } catch (Exception e) {
                html.append("Semnătura: ____________<br>")
                    .append("Data: ____________");
            }
        } else {
            html.append("Semnătura: ____________<br>")
                .append("Data: ____________");
        }
            
        html.append("</td>")
            .append("</tr>")
            .append("</table>");

        html.append("</body></html>");
        return html.toString();
    }
    
 // În ProdecanController, adaugă aceste metode pentru managementul partenerilor

 // Modifică metoda pentru listarea partenerilor
//    @GetMapping("/management/partners")
//    public String listPartners(Model model, Authentication authentication) {
//        User user = (User) authentication.getPrincipal();
//        model.addAttribute("user", user);
//        
//        try {
//            List<Partner> partners = partnerRepository.findAll();
//            System.out.println("DEBUG: Total parteneri găsiți: " + partners.size());
//            
//            // Debug detaliat pentru fiecare partener
//            for (Partner partner : partners) {
//                System.out.println("Partner ID=" + partner.getId() + 
//                                 ", Nume=" + partner.getNume() + 
//                                 ", Email=" + partner.getEmail() + 
//                                 ", Companie=" + (partner.getCompanie() != null ? partner.getCompanie().getNume() : "NULL"));
//            }
//            
//            model.addAttribute("partners", partners);
//        } catch (Exception e) {
//            System.err.println("EROARE la încărcarea partenerilor: " + e.getMessage());
//            e.printStackTrace();
//            model.addAttribute("partners", new ArrayList<>());
//            model.addAttribute("errorMessage", "A apărut o eroare la încărcarea partenerilor: " + e.getMessage());
//        }
//        
//        return "prodecan/management/partners";
//    }
    // Modifică metoda pentru afișarea formularului de creare
    @GetMapping("/management/partners/create")
    public String showCreatePartnerForm(Model model, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        model.addAttribute("user", user);
        
        model.addAttribute("partner", new Partner());
        model.addAttribute("companii", companieRepository.findAll());
        
        return "prodecan/management/create"; // returnează direct create.html
    }
//    @PostMapping("/management/partners/create")
//    public String createPartner(@ModelAttribute Partner partner, RedirectAttributes redirectAttributes) {
//        try {
//            // Validare email
//            if (!ValidationUtils.isValidEmail(partner.getEmail())) {
//                redirectAttributes.addFlashAttribute("errorMessage", 
//                    "Adresa de email nu este validă!");
//                return "redirect:/prodecan/management/partners/create";
//            }
//            
//            // Verificăm dacă există deja un utilizator cu acest email
//            User existingUser = userRepository.findByEmail(partner.getEmail());
//            if (existingUser != null) {
//                redirectAttributes.addFlashAttribute("errorMessage", 
//                    "Există deja un utilizator cu această adresă de email!");
//                return "redirect:/prodecan/management/partners/create";
//            }
//            
//            // Creăm partenerul folosind serviciul
//            PartnerService.PartnerCreationResult result = partnerService.createPartnerWithUser(partner);
//            
//            // Verificăm din nou că partenerul există
//            Partner checkPartner = partnerRepository.findById(result.getPartner().getId());
//            if (checkPartner == null) {
//                throw new RuntimeException("Partenerul nu a fost găsit după salvare!");
//            }
//            
//            redirectAttributes.addFlashAttribute("successMessage", 
//                "Partener creat cu succes!\n" +
//                "----------------------------------------\n" +
//                "Email: " + result.getPartner().getEmail() + "\n" +
//                "PAROLA TEMPORARĂ: " + result.getTemporaryPassword() + "\n" +
//                "----------------------------------------\n" +
//                "IMPORTANT: Salvați această parolă!");
//            
//            return "redirect:/prodecan/management/partners";
//            
//        } catch (Exception e) {
//            System.err.println("EROARE la crearea partenerului: " + e.getMessage());
//            e.printStackTrace();
//            redirectAttributes.addFlashAttribute("errorMessage", 
//                "Eroare la crearea partenerului: " + e.getMessage());
//            return "redirect:/prodecan/management/partners/create";
//        }
//    }
//    // Modifică metoda pentru editarea partenerului
//    @GetMapping("/management/partners/edit/{id}")
//    public String showEditPartnerForm(@PathVariable int id, Model model, Authentication authentication) {
//        User user = (User) authentication.getPrincipal();
//        model.addAttribute("user", user);
//        
//        Partner partner = partnerRepository.findById(id);
//        if (partner == null) {
//            return "redirect:/prodecan/management/partners";
//        }
//        
//        model.addAttribute("partner", partner);
//        model.addAttribute("companii", companieRepository.findAll());
//        return "prodecan/management/partner-form";
//    }

//    @PostMapping("/management/partners/edit/{id}")
//    public String updatePartner(@PathVariable int id, @ModelAttribute Partner partner, RedirectAttributes redirectAttributes) {
//        try {
//            Partner existingPartner = partnerRepository.findById(id);
//            if (existingPartner == null) {
//                redirectAttributes.addFlashAttribute("errorMessage", "Partenerul nu a fost găsit!");
//                return "redirect:/prodecan/management/partners";
//            }
//            
//            // Păstrăm email-ul original și ID-ul
//            partner.setId(id);
//            partner.setEmail(existingPartner.getEmail());
//            
//            // Salvăm partenerul actualizat
//            partnerRepository.save(partner);
//            
//            // Actualizăm și contul de utilizator asociat (dar nu email-ul)
//            User userPartner = userRepository.findByEmail(existingPartner.getEmail());
//            if (userPartner != null) {
//                userPartner.setNume(partner.getNume());
//                userPartner.setPrenume(partner.getPrenume());
//                userRepository.save(userPartner);
//            }
//            
//            redirectAttributes.addFlashAttribute("successMessage", "Partener actualizat cu succes!");
//            return "redirect:/prodecan/management/partners";
//            
//        } catch (Exception e) {
//            e.printStackTrace(); // Pentru debugging
//            redirectAttributes.addFlashAttribute("errorMessage", 
//                "Eroare la actualizarea partenerului: " + e.getMessage());
//            return "redirect:/prodecan/management/partners/edit/" + id;
//        }
//    }

//    @PostMapping("/management/partners/reset-password/{id}")
//    public String resetPartnerPassword(@PathVariable int id, RedirectAttributes redirectAttributes) {
//        try {
//            Partner partner = partnerRepository.findById(id);
//            if (partner != null) {
//                User userPartner = userRepository.findByEmail(partner.getEmail());
//                if (userPartner != null) {
//                    String newPassword = passwordGeneratorService.generateRandomPassword();
//                    userPartner.setPassword(passwordEncoder.encode(newPassword));
//                    userRepository.save(userPartner);
//                    
//                    redirectAttributes.addFlashAttribute("successMessage", 
//                        "Parolă resetată cu succes!\n" +
//                        "----------------------------------------\n" +
//                        "Partener: " + partner.getNume() + " " + partner.getPrenume() + "\n" +
//                        "Email: " + partner.getEmail() + "\n" +
//                        "NOUA PAROLĂ: " + newPassword + "\n" +
//                        "----------------------------------------\n" +
//                        "IMPORTANT: Salvați această parolă!");
//                }
//            }
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("errorMessage", 
//                "Eroare la resetarea parolei: " + e.getMessage());
//        }
//        return "redirect:/prodecan/management/partners";
//    }

//    @GetMapping("/management/partners/delete/{id}")
//    public String deletePartner(@PathVariable int id, RedirectAttributes redirectAttributes) {
//        try {
//            Partner partner = partnerRepository.findById(id);
//            if (partner == null) {
//                redirectAttributes.addFlashAttribute("errorMessage", "Partenerul nu a fost găsit!");
//                return "redirect:/prodecan/management/partners";
//            }
//            
//            // Verificăm dacă există convenții asociate
//            List<Conventie> conventiiAsociate = conventieRepository.findByCompanieId(partner.getCompanie().getId());
//            if (!conventiiAsociate.isEmpty()) {
//                redirectAttributes.addFlashAttribute("errorMessage", 
//                    "Nu se poate șterge partenerul deoarece există convenții asociate companiei!");
//                return "redirect:/prodecan/management/partners";
//            }
//            
//            // Ștergem contul de utilizator asociat
//            User userPartner = userRepository.findByEmail(partner.getEmail());
//            if (userPartner != null) {
//                userRepository.delete(userPartner);
//            }
//            
//            // Ștergem partenerul
//            partnerRepository.delete(partner);
//            redirectAttributes.addFlashAttribute("successMessage", "Partenerul a fost șters cu succes!");
//            
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("errorMessage", 
//                "A apărut o eroare la ștergerea partenerului: " + e.getMessage());
//        }
//        return "redirect:/prodecan/management/partners";
//    }

    // Metode similare pentru edit, delete și reset-password

    // Acum adaugă metode pentru managementul tutorilor

   
//
//    @GetMapping("/management/tutori/create")
//    public String showCreateTutoreForm(Model model, Authentication authentication) {
//        User user = (User) authentication.getPrincipal();
//        model.addAttribute("user", user);
//        model.addAttribute("tutore", new Tutore());
//        model.addAttribute("companii", companieRepository.findAll());
//        return "prodecan/management/tutore-form";
//    }
//
//    @PostMapping("/management/tutori/create")
//    public String createTutore(@ModelAttribute Tutore tutore, RedirectAttributes redirectAttributes) {
//        try {
//            // Validăm email-ul
//            if (!ValidationUtils.isValidEmail(tutore.getEmail())) {
//                redirectAttributes.addFlashAttribute("errorMessage", 
//                    "Adresa de email nu este validă!");
//                return "redirect:/prodecan/management/tutori/create";
//            }
//            
//            // Salvăm tutorele
//            tutoreRepository.save(tutore);
//            
//            // Creăm contul de utilizator
//            String tempPassword = passwordGeneratorService.generateRandomPassword();
//            User userTutore = new User();
//            userTutore.setEmail(tutore.getEmail());
//            userTutore.setNume(tutore.getNume());
//            userTutore.setPrenume(tutore.getPrenume());
//            userTutore.setPassword(passwordEncoder.encode(tempPassword));
//            userTutore.setRole("ROLE_TUTORE");
//            userTutore.setEnabled(true);
//            userTutore.setFirstLogin(true);
//            
//            userRepository.save(userTutore);
//            
//            redirectAttributes.addFlashAttribute("successMessage", 
//                "Tutore creat cu succes!\n" +
//                "----------------------------------------\n" +
//                "Email: " + tutore.getEmail() + "\n" +
//                "PAROLA TEMPORARĂ: " + tempPassword + "\n" +
//                "----------------------------------------\n" +
//                "IMPORTANT: Salvați această parolă!");
//            
//            return "redirect:/prodecan/management/tutori";
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("errorMessage", 
//                "Eroare la crearea tutorelui: " + e.getMessage());
//            return "redirect:/prodecan/management/tutori/create";
//        }
//    }

//    @GetMapping("/management/tutori/edit/{id}")
//    public String showEditTutoreForm(@PathVariable int id, Model model, Authentication authentication) {
//        User user = (User) authentication.getPrincipal();
//        model.addAttribute("user", user);
//        
//        Tutore tutore = tutoreRepository.findById(id);
//        if (tutore == null) {
//            return "redirect:/prodecan/management/tutori";
//        }
//        
//        model.addAttribute("tutore", tutore);
//        model.addAttribute("companii", companieRepository.findAll());
//        return "prodecan/management/tutore-form";
//    }
//
//    @PostMapping("/management/tutori/edit/{id}")
//    public String updateTutore(@PathVariable int id, @ModelAttribute Tutore tutore, RedirectAttributes redirectAttributes) {
//        try {
//            Tutore existingTutore = tutoreRepository.findById(id);
//            if (existingTutore == null) {
//                redirectAttributes.addFlashAttribute("errorMessage", "Tutorele nu a fost găsit!");
//                return "redirect:/prodecan/management/tutori";
//            }
//            
//            // Salvăm email-ul original
//            String originalEmail = existingTutore.getEmail();
//            
//            // Actualizăm tutorele dar păstrăm email-ul neschimbat pentru a menține integritatea contului
//            existingTutore.setNume(tutore.getNume());
//            existingTutore.setPrenume(tutore.getPrenume());
//            existingTutore.setFunctie(tutore.getFunctie());
//            existingTutore.setTelefon(tutore.getTelefon());
//            existingTutore.setCompanie(tutore.getCompanie());
//            
//            tutoreRepository.save(existingTutore);
//            
//            // Actualizăm și contul de utilizator asociat
//            User userTutore = userRepository.findByEmail(originalEmail);
//            if (userTutore != null) {
//                userTutore.setNume(tutore.getNume());
//                userTutore.setPrenume(tutore.getPrenume());
//                userRepository.save(userTutore);
//            }
//            
//            redirectAttributes.addFlashAttribute("successMessage", "Tutore actualizat cu succes!");
//            return "redirect:/prodecan/management/tutori";
//            
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("errorMessage", 
//                "Eroare la actualizarea tutorelui: " + e.getMessage());
//            return "redirect:/prodecan/management/tutori/edit/" + id;
//        }
//    }

//    @PostMapping("/management/tutori/reset-password/{id}")
//    public String resetTutorePassword(@PathVariable int id, RedirectAttributes redirectAttributes) {
//        try {
//            Tutore tutore = tutoreRepository.findById(id);
//            if (tutore != null) {
//                User userTutore = userRepository.findByEmail(tutore.getEmail());
//                if (userTutore != null) {
//                    String newPassword = passwordGeneratorService.generateRandomPassword();
//                    userTutore.setPassword(passwordEncoder.encode(newPassword));
//                    userRepository.save(userTutore);
//                    
//                    redirectAttributes.addFlashAttribute("successMessage", 
//                        "Parolă resetată cu succes!\n" +
//                        "----------------------------------------\n" +
//                        "Tutore: " + tutore.getNume() + " " + tutore.getPrenume() + "\n" +
//                        "Email: " + tutore.getEmail() + "\n" +
//                        "NOUA PAROLĂ: " + newPassword + "\n" +
//                        "----------------------------------------\n" +
//                        "IMPORTANT: Salvați această parolă!");
//                }
//            }
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("errorMessage", 
//                "Eroare la resetarea parolei: " + e.getMessage());
//        }
//        return "redirect:/prodecan/management/tutori";
//    }

//    @GetMapping("/management/tutori/delete/{id}")
//    public String deleteTutore(@PathVariable int id, RedirectAttributes redirectAttributes) {
//        try {
//            Tutore tutore = tutoreRepository.findById(id);
//            if (tutore == null) {
//                redirectAttributes.addFlashAttribute("errorMessage", "Tutorele nu a fost găsit!");
//                return "redirect:/prodecan/management/tutori";
//            }
//            
//            // Verificăm dacă există convenții asociate
//            List<Conventie> conventiiAsociate = conventieRepository.findByCompanieId(tutore.getCompanie().getId());
//            if (!conventiiAsociate.isEmpty()) {
//                redirectAttributes.addFlashAttribute("errorMessage", 
//                    "Nu se poate șterge tutorele deoarece există convenții asociate companiei!");
//                return "redirect:/prodecan/management/tutori";
//            }
//            
//            // Ștergem contul de utilizator asociat
//            User userTutore = userRepository.findByEmail(tutore.getEmail());
//            if (userTutore != null) {
//                userRepository.delete(userTutore);
//            }
//            
//            // Ștergem tutorele
//            tutoreRepository.delete(tutore);
//            redirectAttributes.addFlashAttribute("successMessage", "Tutorele a fost șters cu succes!");
//            
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("errorMessage", 
//                "A apărut o eroare la ștergerea tutorelui: " + e.getMessage());
//        }
//        return "redirect:/prodecan/management/tutori";
//    }
}