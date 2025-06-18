DROP DATABASE IF EXISTS conventii;
CREATE DATABASE conventii
  DEFAULT CHARACTER SET utf8 
  DEFAULT COLLATE utf8_general_ci;

USE conventii;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    nume VARCHAR(100) NOT NULL,
    prenume VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    departament VARCHAR(100),
    facultate VARCHAR(100),
    specializare VARCHAR(100),
    titlu_academic VARCHAR(100),
    first_login BOOLEAN DEFAULT NULL
);

CREATE TABLE student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nume VARCHAR(100) NOT NULL,
    prenume VARCHAR(100),
    cnp VARCHAR(13),
    data_nasterii DATE,
    locul_nasterii VARCHAR(100),
    cetatenie VARCHAR(50),
    serie_ci VARCHAR(10),
    numar_ci VARCHAR(10),
    adresa TEXT,
    an_universitar VARCHAR(20),
    facultate VARCHAR(100),
    specializare VARCHAR(100),
    an_de_studiu INT DEFAULT 1,
    email VARCHAR(100) UNIQUE,
    telefon VARCHAR(20)
);

CREATE TABLE cadru_didactic (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nume VARCHAR(100) NOT NULL,
    prenume VARCHAR(100),
    functie VARCHAR(100),
    specializare VARCHAR(100),
    telefon VARCHAR(20),
    email VARCHAR(100)
);

CREATE TABLE companie (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nume VARCHAR(100) NOT NULL,
    reprezentant VARCHAR(100),
    calitate VARCHAR(100),
    adresa TEXT,
    telefon VARCHAR(20)
);

-- Updated conventie table with proper status constraint
CREATE TABLE conventie (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    companie_id BIGINT,
    student_id BIGINT,
    locul_desfasurarii TEXT,
    durata_in_planul_de_invatamant INTEGER,
    data_inceput DATE,
    data_sfarsit DATE,
    nume_proiect VARCHAR(200),
    tutore_nume VARCHAR(100),
    tutore_prenume VARCHAR(100),
    tutore_functie VARCHAR(100),
    tutore_telefon VARCHAR(20),
    tutore_email VARCHAR(100),
    cadru_didactic_id BIGINT,
    numar_credite INTEGER,
    indemnizatii TEXT,
    avantaje TEXT,
    alte_precizari TEXT,
    data_intocmirii DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_ASTEPTARE_PARTENER',
    FOREIGN KEY (companie_id) REFERENCES companie(id),
    FOREIGN KEY (student_id) REFERENCES student(id),
    FOREIGN KEY (cadru_didactic_id) REFERENCES cadru_didactic(id),
    
    -- Add constraint to ensure only valid enum values
    CONSTRAINT chk_conventie_status CHECK (
        status IN (
            'NETRIMIS', 
            'IN_ASTEPTARE_PARTENER', 
            'APROBATA_PARTENER',
            'IN_ASTEPTARE_TUTORE', 
            'APROBATA_TUTORE', 
            'IN_ASTEPTARE_PRODECAN',
            'IN_ASTEPTARE_PRORECTOR', 
            'APROBATA', 
            'RESPINSA'
        )
    )
);