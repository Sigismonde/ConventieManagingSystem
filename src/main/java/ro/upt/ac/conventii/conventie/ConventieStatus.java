package ro.upt.ac.conventii.conventie;

public enum ConventieStatus {
    NETRIMIS,                 // Convention not sent yet
    IN_ASTEPTARE_PARTENER,    // Convention awaiting partner approval (fost IN_ASTEPTARE)
    APROBATA_PARTENER,        // Approved by partner
    IN_ASTEPTARE_TUTORE,      // Awaiting tutor approval (fost TRIMISA_TUTORE)
    APROBATA_TUTORE,          // Approved by tutor
    IN_ASTEPTARE_PRODECAN,    // Awaiting prodecan approval
    IN_ASTEPTARE_PRORECTOR,   // Awaiting prorector approval
    APROBATA,                 // Fully approved
    RESPINSA                  // Rejected
}