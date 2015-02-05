--
-- Table structure patch for table `PEAK`
--
ALTER TABLE PEAK MODIFY MZ DOUBLE NOT NULL;

--
-- Table structure patch for table `SPECTRUM`
--
ALTER TABLE SPECTRUM DROP COLUMN PARENT_ID;
ALTER TABLE SPECTRUM DROP COLUMN COLLISION_ENERGY;
DROP INDEX PARENT_ID ON SPECTRUM;

--
-- Table structure patch for table `RECORD`
--
ALTER TABLE RECORD MODIFY SMILES TEXT;
ALTER TABLE RECORD MODIFY IUPAC TEXT;
ALTER TABLE RECORD DROP COLUMN PRECURSOR_MZ;

--
-- Table structure patch for table `CH_LINK`
--
DROP TABLE IF EXISTS CH_LINK;
CREATE TABLE CH_LINK (
	ID              CHAR(8)      NOT NULL,
	LINK_NAME       VARCHAR(255) NOT NULL,
	LINK_ID         VARCHAR(255) NOT NULL,
	UNIQUE(ID,LINK_NAME,LINK_ID),
	INDEX(ID)
);

--
-- Table structure patch for table `MOLFILE`
--
ALTER TABLE MOLFILE DROP COLUMN MASTER;

CREATE DATABASE IF NOT EXISTS MassBank_General;
USE MassBank_General;

--
-- Table structure for table `JOB_INFO`
--
CREATE TABLE IF NOT EXISTS JOB_INFO (
	JOB_ID            VARCHAR(36)       NOT NULL,
	STATUS            VARCHAR(20)       NOT NULL,
	TIME_STAMP        VARCHAR(19)       NOT NULL,
	SESSION_ID        VARCHAR(32),
	IP_ADDR           VARCHAR(15),
	MAIL_ADDR         VARCHAR(50),
	QUERY_FILE_NAME   TEXT,
	QUERY_FILE_SIZE   INT(10) UNSIGNED,
	TEMP_FILE_NAME    TEXT,
	SEARCH_PARAM      TEXT,
	RESULT            LONGTEXT,
	PRIMARY KEY(JOB_ID)
);
