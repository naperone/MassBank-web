CREATE DATABASE `SAMPLE_DB`;
USE `SAMPLE_DB`;

--
-- Table structure for table `PEAK`
--
CREATE TABLE PEAK (
	ID          CHAR(8)  NOT NULL,
	MZ          DOUBLE   NOT NULL,
	INTENSITY   FLOAT    NOT NULL,
	RELATIVE    SMALLINT NOT NULL,
	UNIQUE(ID,MZ)
);

--
-- Table structure for table `SPECTRUM`
--
CREATE TABLE SPECTRUM (
	ID          CHAR(8)      NOT NULL,
	NAME        VARCHAR(255) NOT NULL,
	ION         TINYINT      NOT NULL,
	PRECURSOR_MZ SMALLINT UNSIGNED,
	PRIMARY KEY(ID)
);

--
-- Table structure for table `TREE`
--
CREATE TABLE TREE (
	NO          INT UNSIGNED      NOT NULL,
	PARENT      INT UNSIGNED      NOT NULL,
	POS         SMALLINT UNSIGNED NOT NULL,
	SON         SMALLINT          NOT NULL,
	INFO        VARCHAR(127)      NOT NULL,
	ID          CHAR(8),
	INDEX(PARENT),
	INDEX(ID)
);

--
-- Table structure for table `RECORD`
--
CREATE TABLE RECORD (
	ID            CHAR(8) NOT NULL,
	DATE          DATE,
	FORMULA       VARCHAR(30),
	EXACT_MASS    DOUBLE,
	INSTRUMENT_NO TINYINT UNSIGNED,
	SMILES        TEXT,
	IUPAC         TEXT,
	MS_TYPE       VARCHAR(8),
	PRIMARY KEY(ID),
	INDEX(FORMULA, EXACT_MASS, INSTRUMENT_NO)
);

--
-- Table structure for table `CH_NAME`
--
CREATE TABLE CH_NAME (
	ID              CHAR(8)      NOT NULL,
	NAME            VARCHAR(200) NOT NULL,
	INDEX(ID)
);

--
-- Table structure for table `CH_LINK`
--
CREATE TABLE CH_LINK (
	ID              CHAR(8)      NOT NULL,
	LINK_NAME       VARCHAR(255) NOT NULL,
	LINK_ID         VARCHAR(255) NOT NULL,
	UNIQUE(ID,LINK_NAME,LINK_ID),
	INDEX(ID)
);

--
-- Table structure for table `INSTRUMENT`
--
CREATE TABLE INSTRUMENT (
	INSTRUMENT_NO      TINYINT UNSIGNED NOT NULL,
	INSTRUMENT_TYPE    VARCHAR(50)      NOT NULL,
	INSTRUMENT_NAME    VARCHAR(200)     NOT NULL,
	PRIMARY KEY(INSTRUMENT_NO)
);

--
-- Table structure for table `MOLFILE`
--
CREATE TABLE MOLFILE (
	FILE              VARCHAR(8)        NOT NULL,
	NAME              VARCHAR(200)      NOT NULL,
	UNIQUE(NAME,FILE)
);
