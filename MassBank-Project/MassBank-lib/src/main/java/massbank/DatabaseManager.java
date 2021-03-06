package massbank;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.Pair;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import net.sf.jniinchi.INCHI_RET;

public class DatabaseManager {
	
	private final static String driver = "org.mariadb.jdbc.Driver";
//	private final static String user = MassBankEnv.get(MassBankEnv.KEY_DB_USER);
//	private final static String password = MassBankEnv.get(MassBankEnv.KEY_DB_PASSWORD);
	private final static String user = "bird";
	private final static String password = "bird2006";
//	private final static String databaseName = "MassBankNew";
	private String databaseName;
	private final static String dbHostName = getDbHostName();
//	private final static String connectUrl = "jdbc:mysql://" + dbHostName;
	private final String connectUrl;
//	private final static String connectUrl = "jdbc:mysql://" + dbHostName + "/" + databaseName;
	private Connection con;

	private final static String sqlAC_CHROMATOGRAPHY = "SELECT * FROM AC_CHROMATOGRAPHY WHERE RECORD = ?";
	private final static String sqlAC_MASS_SPECTROMETRY = "SELECT * FROM AC_MASS_SPECTROMETRY WHERE RECORD = ?";
	private final static String sqlCH_LINK = "SELECT * FROM CH_LINK WHERE COMPOUND = ?";
	private final static String sqlCOMMENT = "SELECT * FROM COMMENT WHERE RECORD = ?";
	private final static String sqlCOMPOUND = "SELECT * FROM COMPOUND WHERE ID = ?";
	private final static String sqlCOMPOUND_CLASS = "SELECT * FROM COMPOUND_CLASS WHERE ID = ?";
	private final static String sqlCOMPOUND_COMPOUND_CLASS = "SELECT * FROM COMPOUND_COMPOUND_CLASS WHERE COMPOUND = ?";
	private final static String sqlCOMPOUND_NAME = "SELECT * FROM COMPOUND_NAME WHERE COMPOUND = ?";
	private final static String sqlINSTRUMENT = "SELECT * FROM INSTRUMENT WHERE ID = ?";
	private final static String sqlMS_DATA_PROCESSING = "SELECT * FROM MS_DATA_PROCESSING WHERE RECORD = ?";
	private final static String sqlMS_FOCUSED_ION = "SELECT * FROM MS_FOCUSED_ION WHERE RECORD = ?";
	private final static String sqlNAME = "SELECT * FROM NAME WHERE ID = ?";
	private final static String sqlPEAK = "SELECT * FROM PEAK WHERE RECORD = ?";
	private final static String sqlPK_NUM_PEAK = "SELECT * FROM PK_NUM_PEAK WHERE RECORD = ?";
	private final static String sqlRECORD = "SELECT * FROM RECORD WHERE ACCESSION = ?";
	private final static String sqlSAMPLE = "SELECT * FROM SAMPLE WHERE ID = ?";
	private final static String sqlSP_LINK = "SELECT * FROM SP_LINK WHERE SAMPLE = ?";
	private final static String sqlSP_SAMPLE = "SELECT * FROM SP_SAMPLE WHERE SAMPLE = ?";
	private final static String sqlANNOTATION_HEADER = "SELECT * FROM ANNOTATION_HEADER WHERE RECORD = ?";
	private final static String sqlGetContributorFromAccession = 
			"SELECT CONTRIBUTOR.ACRONYM, CONTRIBUTOR.SHORT_NAME, CONTRIBUTOR.FULL_NAME " +
			"FROM CONTRIBUTOR INNER JOIN RECORD ON RECORD.CONTRIBUTOR=CONTRIBUTOR.ID " +
			"WHERE RECORD.ACCESSION = ?";
	
	private final PreparedStatement statementAC_CHROMATOGRAPHY;
	private final PreparedStatement statementAC_MASS_SPECTROMETRY;
	private final PreparedStatement statementCH_LINK;
	private final PreparedStatement statementCOMMENT;
	private final PreparedStatement statementCOMPOUND;
	private final PreparedStatement statementCOMPOUND_CLASS;
	private final PreparedStatement statementCOMPOUND_COMPOUND_CLASS;
	private final PreparedStatement statementCOMPOUND_NAME;
	private final PreparedStatement statementINSTRUMENT;
	private final PreparedStatement statementMS_DATA_PROCESSING;
	private final PreparedStatement statementMS_FOCUSED_ION;
	private final PreparedStatement statementNAME;
	private final PreparedStatement statementPEAK;
	private final PreparedStatement statementPK_NUM_PEAK;
	private final PreparedStatement statementRECORD;
	private final PreparedStatement statementSAMPLE;
	private final PreparedStatement statementSP_LINK;
	private final PreparedStatement statementSP_SAMPLE;
	private final PreparedStatement statementANNOTATION_HEADER;
	private final PreparedStatement statementGetContributorFromAccession;

	private final static String insertCompound = "INSERT INTO COMPOUND VALUES(?,?,?,?,?,?,?,?)";
	private final static String insertCompound_Class = "INSERT INTO COMPOUND_CLASS VALUES(?,?,?,?)";
	private final static String insertCompound_Compound_Class = "INSERT INTO COMPOUND_COMPOUND_CLASS VALUES(?,?)";
	private final static String insertName = "INSERT INTO NAME VALUES(?,?)";
	private final static String insertCompound_Name = "INSERT IGNORE INTO COMPOUND_NAME VALUES(?,?)";
	private final static String insertCH_LINK = "INSERT INTO CH_LINK VALUES(?,?,?)";
	private final static String insertSAMPLE = "INSERT INTO SAMPLE VALUES(?,?,?)";
	private final static String insertSP_LINK = "INSERT INTO SP_LINK VALUES(?,?)";
	private final static String insertSP_SAMPLE = "INSERT INTO SP_SAMPLE VALUES(?,?)";
	private final static String insertINSTRUMENT = "INSERT INTO INSTRUMENT VALUES(?,?,?)";
	private final static String insertRECORD = "INSERT INTO RECORD VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	private final static String insertCOMMENT = "INSERT INTO COMMENT VALUES(?,?)";
	private final static String insertAC_MASS_SPECTROMETRY = "INSERT INTO AC_MASS_SPECTROMETRY VALUES(?,?,?)";
	private final static String insertAC_CHROMATOGRAPHY = "INSERT INTO AC_CHROMATOGRAPHY VALUES(?,?,?)";
	private final static String insertMS_FOCUSED_ION = "INSERT INTO MS_FOCUSED_ION VALUES(?,?,?)";
	private final static String insertMS_DATA_PROCESSING = "INSERT INTO MS_DATA_PROCESSING VALUES(?,?,?)";
//	private final static String insertPEAK = "INSERT INTO PEAK VALUES(?,?,?,?,?,?,?,?)";
	private final static String insertPEAK = "INSERT INTO PEAK VALUES(?,?,?,?,?)";
//	private final static String updatePEAK = "UPDATE PEAK SET PK_ANNOTATION_TENTATIVE_FORMULA = ?, PK_ANNOTATION_FORMULA_COUNT = ?, PK_ANNOTATION_THEORETICAL_MASS = ?, PK_ANNTOATION_ERROR_PPM = ? WHERE RECORD = ? AND PK_PEAK_MZ = ?";
	private final static String updatePEAKs = "UPDATE PEAK SET PK_ANNOTATION = ? WHERE RECORD = ? AND PK_PEAK_MZ = ?";
	private final static String insertANNOTATION_HEADER = "INSERT INTO ANNOTATION_HEADER VALUES(?,?)";
	
	private final PreparedStatement statementInsertCompound;
	private final PreparedStatement statementInsertCompound_Class;
	private final PreparedStatement statementInsertCompound_Compound_Class;
	private final PreparedStatement statementInsertName;
	private final PreparedStatement statementInsertCompound_Name;
	private final PreparedStatement statementInsertCH_LINK;
	private final PreparedStatement statementInsertSAMPLE;
	private final PreparedStatement statementInsertSP_LINK;
	private final PreparedStatement statementInsertSP_SAMPLE;
	private final PreparedStatement statementInsertINSTRUMENT;
	private final PreparedStatement statementInsertRECORD;
	private final PreparedStatement statementInsertCOMMENT;
	private final PreparedStatement statementInsertAC_MASS_SPECTROMETRY;
	private final PreparedStatement statementInsertAC_CHROMATOGRAPHY;
	private final PreparedStatement statementInsertMS_FOCUSED_ION;
	private final PreparedStatement statementInsertMS_DATA_PROCESSING;
	private final PreparedStatement statementInsertPEAK;
//	private final PreparedStatement statementUpdatePEAK;
	private final PreparedStatement statementUpdatePEAKs;
	private final PreparedStatement statementInsertANNOTATION_HEADER;
		
	public static void init_db() throws SQLException, IOException {
		Connection connection = DriverManager.getConnection("jdbc:mariadb://" + dbHostName + "/" + "?user=" + user + "&password=" + password);
		Statement stmt = connection.createStatement();

		stmt.executeUpdate("DROP DATABASE IF EXISTS MassBank;");
		stmt.executeUpdate("CREATE DATABASE MassBank CHARACTER SET = 'latin1' COLLATE = 'latin1_general_cs';");
		stmt.executeUpdate("USE MassBank;");
		
		ClassLoader classLoader = DatabaseManager.class.getClassLoader();
		File file = new File(classLoader.getResource("create_massbank_scheme.sql").getFile());
		ScriptRunner runner = new ScriptRunner(connection, false, false);
		runner.runScript(new BufferedReader(new FileReader(file)));
		
		stmt.close();
		connection.close();
	}
	
	public static DatabaseManager create(String dbName) {
		try {
			return new DatabaseManager(dbName);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static DatabaseManager create() {
		return create("MassBank");
	}
	
	public DatabaseManager(String dbName) throws SQLException {
		this.databaseName = dbName;
		this.connectUrl = "jdbc:mariadb://" + dbHostName + "/" + databaseName + "?rewriteBatchedStatements=true";
			this.openConnection();
			statementAC_CHROMATOGRAPHY = this.con.prepareStatement(sqlAC_CHROMATOGRAPHY);
			statementAC_MASS_SPECTROMETRY = this.con.prepareStatement(sqlAC_MASS_SPECTROMETRY);
			statementCH_LINK = this.con.prepareStatement(sqlCH_LINK);
			statementCOMMENT = this.con.prepareStatement(sqlCOMMENT);
			statementCOMPOUND = this.con.prepareStatement(sqlCOMPOUND);
			statementCOMPOUND_CLASS = this.con.prepareStatement(sqlCOMPOUND_CLASS);
			statementCOMPOUND_COMPOUND_CLASS = this.con.prepareStatement(sqlCOMPOUND_COMPOUND_CLASS);
			statementCOMPOUND_NAME = this.con.prepareStatement(sqlCOMPOUND_NAME);
			statementINSTRUMENT = this.con.prepareStatement(sqlINSTRUMENT);
			statementMS_DATA_PROCESSING = this.con.prepareStatement(sqlMS_DATA_PROCESSING);
			statementMS_FOCUSED_ION = this.con.prepareStatement(sqlMS_FOCUSED_ION);
			statementNAME = this.con.prepareStatement(sqlNAME);
			statementPEAK = this.con.prepareStatement(sqlPEAK);
			statementPK_NUM_PEAK = this.con.prepareStatement(sqlPK_NUM_PEAK);
			statementRECORD = this.con.prepareStatement(sqlRECORD);
			statementSAMPLE = this.con.prepareStatement(sqlSAMPLE);
			statementSP_LINK = this.con.prepareStatement(sqlSP_LINK);
			statementSP_SAMPLE = this.con.prepareStatement(sqlSP_SAMPLE);
			statementANNOTATION_HEADER = this.con.prepareStatement(sqlANNOTATION_HEADER);
			statementGetContributorFromAccession = this.con.prepareStatement(sqlGetContributorFromAccession);
			
			statementInsertCompound = this.con.prepareStatement(insertCompound, Statement.RETURN_GENERATED_KEYS);
			statementInsertCompound_Class = this.con.prepareStatement(insertCompound_Class, Statement.RETURN_GENERATED_KEYS);
			statementInsertCompound_Compound_Class = this.con.prepareStatement(insertCompound_Compound_Class);
			statementInsertName = this.con.prepareStatement(insertName, Statement.RETURN_GENERATED_KEYS);
			statementInsertCompound_Name = this.con.prepareStatement(insertCompound_Name);
			statementInsertCH_LINK = this.con.prepareStatement(insertCH_LINK);
			statementInsertSAMPLE = this.con.prepareStatement(insertSAMPLE, Statement.RETURN_GENERATED_KEYS);
			statementInsertSP_LINK = this.con.prepareStatement(insertSP_LINK);
			statementInsertSP_SAMPLE = this.con.prepareStatement(insertSP_SAMPLE);
			statementInsertINSTRUMENT = this.con.prepareStatement(insertINSTRUMENT, Statement.RETURN_GENERATED_KEYS);
			statementInsertRECORD = this.con.prepareStatement(insertRECORD);
			statementInsertCOMMENT = this.con.prepareStatement(insertCOMMENT);
			statementInsertAC_MASS_SPECTROMETRY = this.con.prepareStatement(insertAC_MASS_SPECTROMETRY);
			statementInsertAC_CHROMATOGRAPHY = this.con.prepareStatement(insertAC_CHROMATOGRAPHY);
			statementInsertMS_FOCUSED_ION = this.con.prepareStatement(insertMS_FOCUSED_ION);
			statementInsertMS_DATA_PROCESSING = this.con.prepareStatement(insertMS_DATA_PROCESSING);
			statementInsertPEAK = this.con.prepareStatement(insertPEAK);
//			statementUpdatePEAK = this.con.prepareStatement(updatePEAK);
			statementUpdatePEAKs = this.con.prepareStatement(updatePEAKs);
			statementInsertANNOTATION_HEADER = this.con.prepareStatement(insertANNOTATION_HEADER);

	}
		
	private void openConnection() {
		Connection con	= null;
		try {
			Class.forName(DatabaseManager.driver);
			con = DriverManager.getConnection(this.connectUrl, DatabaseManager.user, DatabaseManager.password);
			con.setAutoCommit(false);
			con.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);
		} catch (Exception e) {
			e.printStackTrace();
		}
			this.con = con;
	}

	public void closeConnection() {
		try {
			if ( this.con != null ) this.con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}	
	
	public Record getAccessionData(String accessionId, String contributor) {
		Record acc = new Record(contributor);
		try {
			this.statementRECORD.setString(1, accessionId);
			ResultSet set = this.statementRECORD.executeQuery();
			int compoundID = -1;
			int sampleID = -1;
			int instrumentID = -1;
			if (set.next()) {
				acc.ACCESSION(set.getString("ACCESSION"));
				acc.RECORD_TITLE(set.getString("RECORD_TITLE"));
				acc.DATE(set.getDate("DATE").toLocalDate());
				acc.AUTHORS(set.getString("AUTHORS"));
				acc.LICENSE(set.getString("LICENSE"));
				acc.COPYRIGHT(set.getString("COPYRIGHT"));
				acc.PUBLICATION(set.getString("PUBLICATION"));
				compoundID = set.getInt("CH");
				sampleID = set.getInt("SP");
				instrumentID = set.getInt("AC_INSTRUMENT");
				acc.AC_MASS_SPECTROMETRY_MS_TYPE(set.getString("AC_MASS_SPECTROMETRY_MS_TYPE"));
				acc.AC_MASS_SPECTROMETRY_ION_MODE(set.getString("AC_MASS_SPECTROMETRY_ION_MODE"));
				acc.PK_SPLASH(set.getString("PK_SPLASH"));
				this.statementAC_CHROMATOGRAPHY.setString(1, set.getString("ACCESSION"));
				this.statementAC_MASS_SPECTROMETRY.setString(1, set.getString("ACCESSION"));
				this.statementMS_DATA_PROCESSING.setString(1, set.getString("ACCESSION"));
				this.statementMS_FOCUSED_ION.setString(1, set.getString("ACCESSION"));
				this.statementCOMMENT.setString(1, set.getString("ACCESSION"));
				this.statementPEAK.setString(1, set.getString("ACCESSION"));
				this.statementPK_NUM_PEAK.setString(1, set.getString("ACCESSION"));
				this.statementANNOTATION_HEADER.setString(1, accessionId);
				
				ResultSet tmp = this.statementAC_CHROMATOGRAPHY.executeQuery();
				List<Pair<String, String>> tmpList	= new ArrayList<Pair<String, String>>();
				while (tmp.next())
					tmpList.add(Pair.of(tmp.getString("SUBTAG"), tmp.getString("VALUE")));
				acc.AC_CHROMATOGRAPHY(tmpList);
				
				tmp = this.statementAC_MASS_SPECTROMETRY.executeQuery();
				tmpList.clear();
				while (tmp.next())
					tmpList.add(Pair.of(tmp.getString("SUBTAG"), tmp.getString("VALUE")));
				acc.AC_MASS_SPECTROMETRY(tmpList);
				
				tmp = this.statementMS_DATA_PROCESSING.executeQuery();
				tmpList.clear();
				while (tmp.next())
					tmpList.add(Pair.of(tmp.getString("SUBTAG"), tmp.getString("VALUE")));
				acc.MS_DATA_PROCESSING(tmpList);
				
				tmp = this.statementMS_FOCUSED_ION.executeQuery();
				tmpList.clear();
				while (tmp.next())
					tmpList.add(Pair.of(tmp.getString("SUBTAG"), tmp.getString("VALUE")));
				acc.MS_FOCUSED_ION(tmpList);
				
				tmp = this.statementCOMMENT.executeQuery();
				List<String> tmpList2	= new ArrayList<String>();
				while (tmp.next())
					tmpList2.add(tmp.getString("COMMENT"));
				acc.COMMENT(tmpList2);
				
				tmp = this.statementANNOTATION_HEADER.executeQuery();
//				int PK_ANNOTATION_HEADER_numberOfTokens	= -1;
				if (tmp.next()) {
					String PK_ANNOTATION_HEADER	= tmp.getString("HEADER");
					String[] PK_ANNOTATION_HEADER_tokens	= PK_ANNOTATION_HEADER.split(" ");
					acc.PK_ANNOTATION_HEADER(Arrays.asList(PK_ANNOTATION_HEADER_tokens));
//					PK_ANNOTATION_HEADER_numberOfTokens	= PK_ANNOTATION_HEADER_tokens.length;
				}
				
				tmp = this.statementPEAK.executeQuery();
//				acc.add("PK$PEAK", null, "m/z int. rel.int.");
				while (tmp.next()) {
					acc.PK_PEAK_ADD_LINE(Arrays.asList((Double) tmp.getDouble("PK_PEAK_MZ"), (Double) (double) tmp.getFloat("PK_PEAK_INTENSITY"), (Double) (double) tmp.getShort("PK_PEAK_RELATIVE")));
					String PK_ANNOTATION	= tmp.getString("PK_ANNOTATION");
					if(PK_ANNOTATION != null)
						acc.PK_ANNOTATION_ADD_LINE(Arrays.asList(PK_ANNOTATION.split(" ")));
				}
				tmp = this.statementPK_NUM_PEAK.executeQuery();
				while (tmp.next()) {
					acc.PK_NUM_PEAK(Integer.valueOf(tmp.getInt("PK_NUM_PEAK")));
				}
			} else throw new IllegalStateException("accessionId '" + accessionId + "' is not in database");
			if (compoundID == -1)
				throw new IllegalStateException("compoundID is not set");
			this.statementCOMPOUND.setInt(1, compoundID);
			set = this.statementCOMPOUND.executeQuery();
			while (set.next()) {
				String formulaString	= set.getString("CH_FORMULA");
				IMolecularFormula m = MolecularFormulaManipulator.getMolecularFormula(formulaString, DefaultChemObjectBuilder.getInstance());
				acc.CH_FORMULA(m);
				acc.CH_EXACT_MASS(set.getDouble("CH_EXACT_MASS"));
				
				String smilesString	= set.getString("CH_SMILES");
				if (smilesString.equals("N/A")) acc.CH_SMILES(new AtomContainer());
				else {
					IAtomContainer c	= new SmilesParser(DefaultChemObjectBuilder.getInstance()).parseSmiles(smilesString);
					acc.CH_SMILES(c);
				}
				
				String iupacString	= set.getString("CH_IUPAC");
				if (iupacString.equals("N/A")) acc.CH_IUPAC(new AtomContainer());
				else {
					// Get InChIToStructure
					InChIToStructure intostruct = InChIGeneratorFactory.getInstance().getInChIToStructure(iupacString, DefaultChemObjectBuilder.getInstance());
					INCHI_RET ret = intostruct.getReturnStatus();
					if (ret == INCHI_RET.WARNING) {
						// Structure generated, but with warning message
						System.out.println(acc.ACCESSION() + ": InChI warning: " + intostruct.getMessage());
					} 
					else if (ret != INCHI_RET.OKAY) {
						// Structure generation failed
						throw new IllegalArgumentException("Can not parse INCHI string in \"CH$IUPAC\" field. Structure generation failed: " + ret.toString() + " [" + intostruct.getMessage() + "] for " + iupacString);
					}
					IAtomContainer iupac = intostruct.getAtomContainer();
					acc.CH_IUPAC(iupac);
				}
				
				// TODO CH$CDK_DEPICT_SMILES
				// TODO CH$CDK_DEPICT_GENERIC_SMILES
				// TODO CH$CDK_DEPICT_STRUCTURE_SMILES
//				acc.add("CH$CDK_DEPICT_SMILES", null, set.getString("CH_CDK_DEPICT_SMILES"));
//				acc.add("CH$CDK_DEPICT_GENERIC_SMILES", null, set.getString("CH_CDK_DEPICT_GENERIC_SMILES"));
//				acc.add("CH$CDK_DEPICT_STRUCTURE_SMILES", null, set.getString("CH_CDK_DEPICT_STRUCTURE_SMILES"));
			}
			this.statementCH_LINK.setInt(1, compoundID);
			set = this.statementCH_LINK.executeQuery();
			List<Pair<String, String>> tmpList	= new ArrayList<Pair<String, String>>();
			while (set.next()) {
				tmpList.add(Pair.of(set.getString("DATABASE_NAME"), set.getString("DATABASE_ID")));
			}
			acc.CH_LINK(tmpList);
			
			this.statementCOMPOUND_COMPOUND_CLASS.setInt(1, compoundID);
			set = this.statementCOMPOUND_COMPOUND_CLASS.executeQuery();
			List<String> tmpList2	= new ArrayList<String>();
			while (set.next()) {
				this.statementCOMPOUND_CLASS.setInt(1, set.getInt("CLASS"));
				ResultSet tmp = this.statementCOMPOUND_CLASS.executeQuery();
				while (tmp.next()) {
					tmpList2.add(tmp.getString("CH_COMPOUND_CLASS"));
				}
			}
			acc.CH_COMPOUND_CLASS(tmpList2);
			
			this.statementCOMPOUND_NAME.setInt(1, compoundID);
			set = this.statementCOMPOUND_NAME.executeQuery();
			tmpList2.clear();
			while (set.next()) {
				this.statementNAME.setInt(1, set.getInt("NAME"));
				ResultSet tmp = this.statementNAME.executeQuery();
				while (tmp.next()) {
					tmpList2.add(tmp.getString("CH_NAME"));
				}
			}
			acc.CH_NAME(tmpList2);
			
			this.statementSAMPLE.setInt(1,sampleID);
			set = this.statementSAMPLE.executeQuery();
			if (set.next()) {
				acc.SP_SCIENTIFIC_NAME(set.getString("SP_SCIENTIFIC_NAME"));
				acc.SP_LINEAGE(set.getString("SP_LINEAGE"));
				
				this.statementSP_LINK.setInt(1,set.getInt("ID"));
				ResultSet tmp = this.statementSP_LINK.executeQuery();
				tmpList.clear();
				while (tmp.next()) {
					String spLink	= tmp.getString("SP_LINK");
					String[] tokens	= spLink.split(" ");
					tmpList.add(Pair.of(tokens[0], tokens[1]));
				}
				acc.SP_LINK(tmpList);
				
				this.statementSP_SAMPLE.setInt(1, set.getInt("ID"));
				tmp = this.statementSP_SAMPLE.executeQuery();
				tmpList2.clear();
				while (tmp.next()) {
					tmpList2.add(tmp.getString("SP_SAMPLE"));
				}
				acc.SP_SAMPLE(tmpList2);
			}
			if (instrumentID == -1)	throw new IllegalStateException("instrumentID is not set");
			this.statementINSTRUMENT.setInt(1, instrumentID);
			set = this.statementINSTRUMENT.executeQuery();
			if (set.next()) {
				acc.AC_INSTRUMENT(set.getString("AC_INSTRUMENT"));
				acc.AC_INSTRUMENT_TYPE(set.getString("AC_INSTRUMENT_TYPE"));
			} else	throw new IllegalStateException("instrumentID is not in database");
		} catch (Exception e) {
			System.out.println("error: " + accessionId);
			e.printStackTrace();
			return null;
		}
//		this.openConnection();
		
		return acc;
	}
	public Record.Structure getStructureOfAccession(String accessionId) {
		String CH_SMILES	= null;
		String CH_IUPAC		= null;
		
		try {
			this.statementRECORD.setString(1, accessionId);
			ResultSet set = this.statementRECORD.executeQuery();
			int compoundID = -1;
			if (set.next()) {
				compoundID = set.getInt("CH");
			} else throw new IllegalStateException("accessionId '" + accessionId + "' is not in database");
			
			if (compoundID == -1)
				throw new IllegalStateException("compoundID is not set");
			this.statementCOMPOUND.setInt(1, compoundID);
			set = this.statementCOMPOUND.executeQuery();
			while (set.next()) {
				String smilesString	= set.getString("CH_SMILES");
				if (!smilesString.equals("N/A")) CH_SMILES	= smilesString;
				
				String iupacString	= set.getString("CH_IUPAC");
				if (!iupacString.equals("N/A")) CH_IUPAC	= iupacString;
			}
		} catch (Exception e) {
			System.out.println("error: " + accessionId);
			e.printStackTrace();
			return null;
		}
		
		return new Record.Structure(CH_SMILES, CH_IUPAC);
	}
	
//	public AccessionData getAccessionData(String accessionId) {
//		AccessionData acc = new AccessionData();
//		try {
//			this.statementRECORD.setString(1, accessionId);
//			ResultSet set = this.statementRECORD.executeQuery();
//			int fkCH = -1;
//			int fkSP = -1;
//			int fkAC_INSTRUMENT = -1;
//			while (set.next()) {
//				acc.add("ACCESSION", null, set.getString("ACCESSION"));
//				acc.add("RECORD_TITLE", null, set.getString("RECORD_TITLE"));
//				acc.add("DATE", null, set.getString("DATE"));
//				acc.add("AUTHORS", null, set.getString("AUTHORS"));
//				acc.add("LICENSE", null, set.getString("LICENSE"));
//				acc.add("COPYRIGHT", null, set.getString("COPYRIGHT"));
//				acc.add("PUBLICATION", null, set.getString("PUBLICATION"));
//				fkCH = set.getInt("CH");
//				fkSP = set.getInt("SP");
//				fkAC_INSTRUMENT = set.getInt("AC_INSTRUMENT");
//				acc.add("AC$MASS_SPECTROMETRY", "MS_TYPE", set.getString("AC_MASS_SPECTROMETRY_MS_TYPE"));
//				acc.add("AC$MASS_SPECTROMETRY", "ION_MODE", set.getString("AC_MASS_SPECTROMETRY_ION_MODE"));
//				acc.add("PK$SPLASH", null, set.getString("PK_SPLASH"));
//				this.statementAC_CHROMATOGRAPHY.setString(1, set.getString("ACCESSION"));
//				this.statementAC_MASS_SPECTROMETRY.setString(1, set.getString("ACCESSION"));
//				this.statementMS_DATA_PROCESSING.setString(1, set.getString("ACCESSION"));
//				this.statementMS_FOCUSED_ION.setString(1, set.getString("ACCESSION"));
//				this.statementCOMMENT.setString(1, set.getString("ACCESSION"));
//				this.statementPEAK.setString(1, set.getString("ACCESSION"));
//				this.statementPK_NUM_PEAK.setString(1, set.getString("ACCESSION"));
//				ResultSet tmp = this.statementAC_CHROMATOGRAPHY.executeQuery();
//				while (tmp.next()) {
//					acc.add("AC$CHROMATOGRAPHY", tmp.getString("SUBTAG"), tmp.getString("VALUE"));
//				}
//				tmp = this.statementAC_MASS_SPECTROMETRY.executeQuery();
//				while (tmp.next()) {
//					acc.add("AC$MASS_SPECTROMETRY", tmp.getString("SUBTAG"), tmp.getString("VALUE"));
//				}
//				tmp = this.statementMS_DATA_PROCESSING.executeQuery();
//				while (tmp.next()) {
//					acc.add("MS$DATA_PROCESSING", tmp.getString("SUBTAG"), tmp.getString("VALUE"));
//				}
//				tmp = this.statementMS_FOCUSED_ION.executeQuery();
//				while (tmp.next()) {
//					acc.add("MS$FOCUSED_ION", tmp.getString("SUBTAG"), tmp.getString("VALUE"));
//				}
//				tmp = this.statementCOMMENT.executeQuery();
//				while (tmp.next()) {
//					acc.add("COMMENT", null, tmp.getString("COMMENT"));
//				}
//				tmp = this.statementPEAK.executeQuery();
////				acc.add("PK$PEAK", null, "m/z int. rel.int.");
//				while (tmp.next()) {
//					acc.add("PK$PEAK", null, tmp.getDouble("PK_PEAK_MZ") + " " + tmp.getFloat("PK_PEAK_INTENSITY") + " " + tmp.getShort("PK_PEAK_RELATIVE"));
//					acc.add("PK$ANNOTATION", null, tmp.getString("PK_ANNOTATION"));
//				}
//				this.statementANNOTATION_HEADER.setString(1, accessionId);
//				tmp = this.statementANNOTATION_HEADER.executeQuery();
//				while (tmp.next()) {
//					acc.annotationHeader = tmp.getString("HEADER");
//				}
//				tmp = this.statementPK_NUM_PEAK.executeQuery();
//				while (tmp.next()) {
//					acc.add("PK$NUM_PEAK", null, Integer.valueOf(tmp.getInt("PK_NUM_PEAK")).toString());
//				}
//			}
//			if (fkCH == -1)
//				return null;
//			if (fkCH != -1)
//				this.statementCOMPOUND.setInt(1, fkCH);
//			set = this.statementCOMPOUND.executeQuery();
//			while (set.next()) {
//				acc.add("CH$FORMULA", null, set.getString("CH_FORMULA"));
//				acc.add("CH$EXACT_MASS", null, set.getString("CH_EXACT_MASS"));
//				acc.add("CH$SMILES", null, set.getString("CH_SMILES"));
//				acc.add("CH$IUPAC", null, set.getString("CH_IUPAC"));
//				acc.add("CH$CDK_DEPICT_SMILES", null, set.getString("CH_CDK_DEPICT_SMILES"));
//				acc.add("CH$CDK_DEPICT_GENERIC_SMILES", null, set.getString("CH_CDK_DEPICT_GENERIC_SMILES"));
//				acc.add("CH$CDK_DEPICT_STRUCTURE_SMILES", null, set.getString("CH_CDK_DEPICT_STRUCTURE_SMILES"));
//			}
//			this.statementCH_LINK.setInt(1, fkCH);
//			set = this.statementCH_LINK.executeQuery();
//			while (set.next()) {
//				acc.add("CH$LINK", set.getString("DATABASE_NAME"), set.getString("DATABASE_ID"));
//			}
//			this.statementCOMPOUND_COMPOUND_CLASS.setInt(1, fkCH);
//			set = this.statementCOMPOUND_COMPOUND_CLASS.executeQuery();
//			while (set.next()) {
//				this.statementCOMPOUND_CLASS.setInt(1, set.getInt("CLASS"));
//				ResultSet tmp = this.statementCOMPOUND_CLASS.executeQuery();
//				while (tmp.next()) {
//					acc.add("CH$COMPOUND_CLASS", null, tmp.getString("CH_COMPOUND_CLASS"));
//				}
//			}
//			this.statementCOMPOUND_NAME.setInt(1, fkCH);
//			set = this.statementCOMPOUND_NAME.executeQuery();
//			while (set.next()) {
//				this.statementNAME.setInt(1, set.getInt("NAME"));
//				ResultSet tmp = this.statementNAME.executeQuery();
//				while (tmp.next()) {
//					acc.add("CH$NAME", null, tmp.getString("CH_NAME"));
//				}
//			}
//			this.statementSAMPLE.setInt(1,fkSP);
//			set = this.statementSAMPLE.executeQuery();
//			while (set.next()) {
//				acc.add("SP$SCIENTIFIC_NAME",null,set.getString("SP_SCIENTIFIC_NAME"));
//				acc.add("SP_LINEAGE", null, set.getString("SP_LINEAGE"));
//				this.statementSP_LINK.setInt(1,set.getInt("ID"));
//				this.statementSP_SAMPLE.setInt(1, set.getInt("ID"));
//				ResultSet tmp = this.statementSP_LINK.executeQuery();
//				while (tmp.next()) {
//					acc.add("SP$LINK", null, tmp.getString("SP_LINK"));
//				}
//				tmp = this.statementSP_SAMPLE.executeQuery();
//				while (tmp.next()) {
//					acc.add("SP$SAMPLE", null, tmp.getString("SP_SAMPLE"));
//				}
//			}
//			if (fkAC_INSTRUMENT != -1)
//				this.statementINSTRUMENT.setInt(1, fkAC_INSTRUMENT);
//			set = this.statementINSTRUMENT.executeQuery();
//			while (set.next()) {
//				acc.add("AC$INSTRUMENT", null, set.getString("AC_INSTRUMENT"));
//				acc.add("AC$INSTRUMENT_TYPE", null, set.getString("AC_INSTRUMENT_TYPE"));
//			}	
//		} catch (SQLException e) {
//			System.out.println("error: " + accessionId);
//			e.printStackTrace();
//			return null;
//		}
////		this.openConnection();
//		
//		return acc;
//	}
	
	private static String getDbHostName() {
		String dbHostName = MassBankEnv.get(MassBankEnv.KEY_DB_HOST_NAME);
		if ( !MassBankEnv.get(MassBankEnv.KEY_DB_MASTER_NAME).equals("") ) {
			dbHostName = MassBankEnv.get(MassBankEnv.KEY_DB_MASTER_NAME);
		}
		return dbHostName;
	}
	
	private HashMap<String,String> getDatabaseOfAccessions() {
		GetConfig config = new GetConfig(MassBankEnv.get(MassBankEnv.KEY_BASE_URL));
		String[] dbNames = config.getDbName();
		HashMap<String,String> dbMapping = new HashMap<String,String>();
		Connection con	= null;
		try {
			Class.forName(driver);
			con = DriverManager.getConnection(connectUrl, user, password);
			con.setAutoCommit(false);
			con.setTransactionIsolation(java.sql.Connection.TRANSACTION_READ_COMMITTED);
			for (String db : dbNames) {
				String sql = "SELECT ACCESSION FROM " + db + ".RECORD";
				PreparedStatement stmnt = con.prepareStatement(sql);
				ResultSet resultSet	= stmnt.executeQuery();
				while (resultSet.next()) {
					dbMapping.put(resultSet.getString("ACCESSION"), db);
				}
				resultSet.close();
			} 
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if ( con != null )
				try {
					con.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
		return dbMapping;
	}
	
	public void batchPersist(ArrayList<Record> accs) {
		for (Record acc : accs) {
			if (acc != null) {
//				if (acc.isValid()) {							
					try {
						persistAccessionFile(acc, true);
					} catch (Exception e) {
						e.printStackTrace();
					}
//				} else {
//				}
			}
		}
//		this.closeConnection();
	}
	
	public void persistAccessionFile(Record acc) {
		persistAccessionFile(acc, false);
	}
	
	// TODO remove contributor sql statements from within the function
	public void persistAccessionFile(Record acc, boolean bulk) {
//		this.openConnection();
//		String insertCompound = "INSERT INTO COMPOUND VALUES(?,?,?,?,?,?,?,?)";
//		PreparedStatement stmnt = con.prepareStatement(insertCompound);
		
		Integer conId = -1;
		try {
			String sql = "SELECT ID FROM CONTRIBUTOR WHERE SHORT_NAME = ?";
			PreparedStatement stmnt = con.prepareStatement(sql);
			stmnt.setString(1, acc.CONTRIBUTOR());
			ResultSet res = stmnt.executeQuery();
			if (res.next()) {
				conId = res.getInt(1);
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		
		if(conId == -1) {
			try {
				String sql = "INSERT INTO CONTRIBUTOR (ACRONYM, SHORT_NAME, FULL_NAME) VALUES (NULL,?,NULL)";
				PreparedStatement stmnt = con.prepareStatement(sql);
				stmnt.setString(1, acc.CONTRIBUTOR());
				stmnt.executeUpdate();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			
			try {
				String sql = "SELECT ID FROM CONTRIBUTOR WHERE SHORT_NAME = ?";
				PreparedStatement stmnt = con.prepareStatement(sql);
				stmnt.setString(1, acc.CONTRIBUTOR());
				ResultSet res = stmnt.executeQuery();
				if (res.next()) {
					conId = res.getInt(1);
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}
		
		try {
		//System.out.println(System.nanoTime());
		statementInsertCompound.setNull(1, java.sql.Types.INTEGER);
		statementInsertCompound.setString(2, acc.CH_FORMULA());
		statementInsertCompound.setDouble(3, acc.CH_EXACT_MASS());
		statementInsertCompound.setString(4, acc.CH_SMILES());
		statementInsertCompound.setString(5, acc.CH_IUPAC());
		
		// TODO support CH$CDK_DEPICT_SMILES
		// TODO support CH$CDK_DEPICT_GENERIC_SMILES
		// TODO support CH$CDK_DEPICT_STRUCTURE_SMILES
//		if (acc.get("CH$CDK_DEPICT_SMILES").size() != 0) {
//			statementInsertCompound.setString(6, acc.get("CH$CDK_DEPICT_SMILES").get(0)[2]);
//		} else {
			statementInsertCompound.setNull(6, java.sql.Types.VARCHAR);
//		}
//		if (acc.get("CH$CDK_DEPICT_GENERIC_SMILES").size() != 0) {
//			statementInsertCompound.setString(7, acc.get("CH$CDK_DEPICT_GENERIC_SMILES").get(0)[2]);
//		} else {
			statementInsertCompound.setNull(7, java.sql.Types.VARCHAR);
//		}
//		if (acc.get("CH$CDK_DEPICT_STRUCTURE_SMILES").size() != 0) {
//			statementInsertCompound.setString(8, acc.get("CH$CDK_DEPICT_STRUCTURE_SMILES").get(0)[2]);
//		} else {
			statementInsertCompound.setNull(8, java.sql.Types.VARCHAR);
//		}
		statementInsertCompound.executeUpdate();
		ResultSet set = statementInsertCompound.getGeneratedKeys();
		set.next();
		int compoundId = set.getInt("ID");
		
		
		//System.out.println(System.nanoTime());
		int compoundClassId;
//		String insertCompoundClass = "INSERT INTO COMPOUND_CLASS VALUES(?,?,?,?)";
//		stmnt = con.prepareStatement(insertCompoundClass);
		for (String el : acc.CH_COMPOUND_CLASS()) {
			statementInsertCompound_Class.setNull(1, java.sql.Types.INTEGER);		
			statementInsertCompound_Class.setString(2, null);
			statementInsertCompound_Class.setString(3, null);
			statementInsertCompound_Class.setString(4, el);
			statementInsertCompound_Class.executeUpdate();
			set = statementInsertCompound_Class.getGeneratedKeys();
			set.next();
			compoundClassId = set.getInt("ID");
			
//			String insertCompoundCompoundClass = "INSERT INTO COMPOUND_COMPOUND_CLASS VALUES(?,?)";
//			stmnt = con.prepareStatement(insertCompoundCompoundClass);
			statementInsertCompound_Compound_Class.setInt(1, compoundId);
			statementInsertCompound_Compound_Class.setInt(2, compoundClassId);
			statementInsertCompound_Compound_Class.executeUpdate();
		}
		
		//System.out.println(System.nanoTime());
		int nameId;
//		String insertName = "INSERT INTO NAME VALUES(?,?)";
//		stmnt = con.prepareStatement(insertName);
		for (String el : acc.CH_NAME()) {
			statementInsertName.setNull(1, java.sql.Types.INTEGER);
			statementInsertName.setString(2, el);
			try {
				statementInsertName.executeUpdate();
				set = statementInsertName.getGeneratedKeys();
				set.next();
				nameId = set.getInt("ID");

//				String insertCompoundName = "INSERT INTO COMPOUND_NAME VALUES(?,?)";
//				stmnt = con.prepareStatement(insertCompoundName);
				statementInsertCompound_Name.setInt(1, compoundId);
				statementInsertCompound_Name.setInt(2, nameId);
				statementInsertCompound_Name.executeUpdate();
			} catch (SQLException e) {
				if (e.getErrorCode() == 1062) {
					PreparedStatement retrieveIdForName = con.prepareStatement("SELECT ID FROM NAME WHERE CH_NAME = ?");
					retrieveIdForName.setString(1, el);
					set = retrieveIdForName.executeQuery();
					set.next();
					nameId = set.getInt("ID");
					statementInsertCompound_Name.setInt(1, compoundId);
					statementInsertCompound_Name.setInt(2, nameId);
					statementInsertCompound_Name.executeUpdate();
				} else {
					this.closeConnection();
					throw e;
//					e.printStackTrace();
//					nameId = -1;
				}
			}
		}
		
		//System.out.println(System.nanoTime());
//		String insertChLink = "INSERT INTO CH_LINK VALUES(?,?,?)";
//		stmnt = con.prepareStatement(insertChLink);
		for (Pair<String, String> el : acc.CH_LINK()) {
			statementInsertCH_LINK.setInt(1,compoundId);
			statementInsertCH_LINK.setString(2, el.getLeft());
			statementInsertCH_LINK.setString(3, el.getRight());
//			statementInsertCH_LINK.executeUpdate();
			statementInsertCH_LINK.addBatch();
		}
		if (!bulk) {
			statementInsertCH_LINK.executeBatch();
		}
		
		//System.out.println(System.nanoTime());
		int sampleId = -1;
		statementInsertSAMPLE.setNull(1, java.sql.Types.INTEGER);
		if (acc.SP_SCIENTIFIC_NAME() != null) {
			statementInsertSAMPLE.setString(2, acc.SP_SCIENTIFIC_NAME());
		} else {
			statementInsertSAMPLE.setNull(2, java.sql.Types.VARCHAR);
		}
		if (acc.SP_LINEAGE() != null) {
			statementInsertSAMPLE.setString(3, acc.SP_LINEAGE());
		} else {
			statementInsertSAMPLE.setNull(3, java.sql.Types.VARCHAR);
		}
		if (acc.SP_SCIENTIFIC_NAME() != null && acc.SP_LINEAGE() != null) {
			statementInsertSAMPLE.executeUpdate();
			set = statementInsertSAMPLE.getGeneratedKeys();
			set.next();
			sampleId = set.getInt("ID");
		}
		
		//System.out.println(System.nanoTime());
		for (Pair<String, String> el : acc.SP_LINK()) {
			statementInsertSP_LINK.setInt(1, sampleId);
			statementInsertSP_LINK.setString(2, el.getLeft() + " " + el.getRight());
//			statementInsertSP_LINK.executeUpdate();
			statementInsertSP_LINK.addBatch();
		}
		if (!bulk) {
			statementInsertSP_LINK.executeBatch();
		}
		
		//System.out.println(System.nanoTime());
		for (String el : acc.SP_SAMPLE()) {
			statementInsertSP_SAMPLE.setInt(1, sampleId);
			statementInsertSP_SAMPLE.setString(2, el);
//			statementInsertSP_SAMPLE.executeUpdate();
			statementInsertSP_SAMPLE.addBatch();
		}
		if (!bulk) {
			statementInsertSP_SAMPLE.executeBatch();
		}
		
		//System.out.println(System.nanoTime());
		statementInsertINSTRUMENT.setNull(1, java.sql.Types.INTEGER);
		statementInsertINSTRUMENT.setString(2, acc.AC_INSTRUMENT());
		statementInsertINSTRUMENT.setString(3, acc.AC_INSTRUMENT_TYPE());
		statementInsertINSTRUMENT.executeUpdate();
		set = statementInsertINSTRUMENT.getGeneratedKeys();
		set.next();
		int instrumentId = set.getInt("ID");
		
		//System.out.println(System.nanoTime());
		statementInsertRECORD.setString(1, acc.ACCESSION());
		statementInsertRECORD.setString(2, acc.RECORD_TITLE());
		statementInsertRECORD.setDate(3, Date.valueOf(acc.DATE()));
		statementInsertRECORD.setString(4, acc.AUTHORS());
//		if (acc.get("LICENSE").size() != 0) {
			statementInsertRECORD.setString(5, acc.LICENSE());			
//		} else {
//			statementInsertRECORD.setNull(5, java.sql.Types.VARCHAR);
//		}
		if (acc.COPYRIGHT() != null) {
			statementInsertRECORD.setString(6, acc.COPYRIGHT());			
		} else {
			statementInsertRECORD.setNull(6, java.sql.Types.VARCHAR);
		}
		if (acc.PUBLICATION() != null) {
			statementInsertRECORD.setString(7, acc.PUBLICATION());			
		} else {
			statementInsertRECORD.setNull(7, java.sql.Types.VARCHAR);
		}
		statementInsertRECORD.setInt(8, compoundId);
		if (sampleId > 0) {
			statementInsertRECORD.setInt(9, sampleId);
		} else {
			statementInsertRECORD.setNull(9, java.sql.Types.INTEGER);
		}
		statementInsertRECORD.setInt(10, instrumentId);
		statementInsertRECORD.setString(11, acc.AC_MASS_SPECTROMETRY_MS_TYPE());
		statementInsertRECORD.setString(12, acc.AC_MASS_SPECTROMETRY_ION_MODE());
		statementInsertRECORD.setString(13, acc.PK_SPLASH());
		statementInsertRECORD.setInt(14, conId);
		statementInsertRECORD.executeUpdate();
//		set = statementInsertRECORD.getGeneratedKeys();
//		set.next();
		String accession = acc.ACCESSION();
		
		//System.out.println(System.nanoTime());
		for (String el : acc.COMMENT()) {
			statementInsertCOMMENT.setString(1, accession);
			statementInsertCOMMENT.setString(2, el);
//			statementInsertCOMMENT.executeUpdate();
			statementInsertCOMMENT.addBatch();
		}
		if (!bulk) {
			statementInsertCOMMENT.executeBatch();
		}
		
		//System.out.println(System.nanoTime());
		for (Pair<String, String> el : acc.AC_MASS_SPECTROMETRY()) {
			statementInsertAC_MASS_SPECTROMETRY.setString(1, accession);
			statementInsertAC_MASS_SPECTROMETRY.setString(2, el.getLeft());
			statementInsertAC_MASS_SPECTROMETRY.setString(3, el.getRight());
//			statementInsertAC_MASS_SPECTROMETRY.executeUpdate();
			statementInsertAC_MASS_SPECTROMETRY.addBatch();
		}
		if (!bulk) {
			statementInsertAC_MASS_SPECTROMETRY.executeBatch();
		}
		
		//System.out.println(System.nanoTime());
		for (Pair<String, String> el : acc.AC_CHROMATOGRAPHY()) {
			statementInsertAC_CHROMATOGRAPHY.setString(1, accession);
			statementInsertAC_CHROMATOGRAPHY.setString(2, el.getLeft());
			statementInsertAC_CHROMATOGRAPHY.setString(3, el.getRight());
//			statementInsertAC_CHROMATOGRAPHY.executeUpdate();
			statementInsertAC_CHROMATOGRAPHY.addBatch();
		}
		if (!bulk) {
			statementInsertAC_CHROMATOGRAPHY.executeBatch();
		}
		
		//System.out.println(System.nanoTime());
		for (Pair<String, String> el : acc.MS_FOCUSED_ION()) {
			statementInsertMS_FOCUSED_ION.setString(1, accession);
			statementInsertMS_FOCUSED_ION.setString(2, el.getLeft());
			statementInsertMS_FOCUSED_ION.setString(3, el.getRight());
//			statementInsertMS_FOCUSED_ION.executeUpdate();
			statementInsertMS_FOCUSED_ION.addBatch();
		}
		if (!bulk) {
			statementInsertMS_FOCUSED_ION.executeBatch();
		}
		
		//System.out.println(System.nanoTime());
		for (Pair<String, String> el : acc.MS_DATA_PROCESSING()) {
			statementInsertMS_DATA_PROCESSING.setString(1, accession);
			statementInsertMS_DATA_PROCESSING.setString(2, el.getLeft());
			statementInsertMS_DATA_PROCESSING.setString(3, el.getRight());
//			statementInsertMS_DATA_PROCESSING.executeUpdate();
			statementInsertMS_DATA_PROCESSING.addBatch();
		}
		if (!bulk) {
			statementInsertMS_DATA_PROCESSING.executeBatch();
		}

		//System.out.println(System.nanoTime());
		for (List<Double> peak : acc.PK_PEAK()) {
			statementInsertPEAK.setString(1, accession);
			statementInsertPEAK.setDouble(2, peak.get(0));
			statementInsertPEAK.setFloat(3, (float)(double) peak.get(1));
			statementInsertPEAK.setShort(4, (short)(double) peak.get(2));
			statementInsertPEAK.setNull(5, java.sql.Types.VARCHAR);
//			statementInsertPEAK.setNull(5, java.sql.Types.VARCHAR);
//			statementInsertPEAK.setNull(6, java.sql.Types.SMALLINT);
//			statementInsertPEAK.setNull(7, java.sql.Types.FLOAT);
//			statementInsertPEAK.setNull(8, java.sql.Types.FLOAT);
//			statementInsertPEAK.executeUpdate();
			statementInsertPEAK.addBatch();
		}
		if (!bulk) {
			statementInsertPEAK.executeBatch();
		}
		
		//System.out.println(System.nanoTime());
		List<List<String>> annotation = acc.PK_ANNOTATION();
		if (annotation.size() != 0) {
			statementInsertANNOTATION_HEADER.setString(1, accession);
			statementInsertANNOTATION_HEADER.setString(2, String.join(" ", annotation.get(0)));
			statementInsertANNOTATION_HEADER.executeUpdate();
		}
		for (int i = 1; i < annotation.size(); i++) {
			String values = String.join(" ", annotation.get(i));
			Float mz = Float.parseFloat(annotation.get(i).get(0));
//			values = values.substring(values.indexOf(" ")+1, values.length());
			statementUpdatePEAKs.setString(1, values);
			statementUpdatePEAKs.setString(2, accession);
			statementUpdatePEAKs.setFloat(3, mz);
//			statementUpdatePEAK.setString(1, values.substring(0, values.indexOf(" ")));
//			values = values.substring(values.indexOf(" ")+1, values.length());
//			statementUpdatePEAK.setShort(2, Short.parseShort(values.substring(0, values.indexOf(" "))));
//			values = values.substring(values.indexOf(" ")+1, values.length());
//			statementUpdatePEAK.setFloat(3, Float.parseFloat(values.substring(0, values.indexOf(" "))));
//			values = values.substring(values.indexOf(" ")+1, values.length());
//			statementUpdatePEAK.setFloat(4, Float.parseFloat(values.substring(0, values.length())));
//			statementUpdatePEAK.setString(5, accession);
//			statementUpdatePEAK.setFloat(6, mz);
//			statementUpdatePEAK.executeUpdate();
			statementUpdatePEAKs.addBatch();
		}
		if (!bulk) {
			statementUpdatePEAKs.executeBatch();
		}
		
		//System.out.println(System.nanoTime());
		con.commit();
		//System.out.println(System.nanoTime());
		
		} catch (SQLException e) {
			StringBuilder tmp = new StringBuilder();
			tmp.append(e.getMessage());
			tmp.append("\n");
			for (StackTraceElement el : e.getStackTrace()) {
				tmp.append(el.toString());
				tmp.append("\n");
			}
			DevLogger.printToDBLog("DB ERROR " + tmp + " for accession: " + acc.ACCESSION());
//			try {
//				e.printStackTrace(new PrintStream(new FileOutputStream("/Users/laptop/Desktop/errors/" + acc.ACCESSION() + ".txt")));
//			} catch (FileNotFoundException e1) {
//				//e1.printStackTrace();
//			}
			this.closeConnection();
		} catch (IndexOutOfBoundsException e) {
			StringBuilder tmp = new StringBuilder();
			tmp.append(e.getMessage());
			tmp.append("\n");
			for (StackTraceElement el : e.getStackTrace()) {
				tmp.append(el.toString());
				tmp.append("\n");
			}
			DevLogger.printToDBLog("DB ERROR " + tmp + " for accession: " + acc.ACCESSION());
//			System.out.println(acc.ACCESSION());
//			System.out.println(acc.get("PK$PEAK").size());
//			System.out.println(acc.get("PK$ANNOTATION").size());
//			try {
//				e.printStackTrace(new PrintStream(new FileOutputStream("/Users/laptop/Desktop/errors/" + acc.ACCESSION() + ".txt")));
//			} catch (FileNotFoundException e1) {
//				//e1.printStackTrace();
//			}
		} catch (Exception e) {
			StringBuilder tmp = new StringBuilder();
			tmp.append(e.getMessage());
			tmp.append("\n");
			for (StackTraceElement el : e.getStackTrace()) {
				tmp.append(el.toString());
				tmp.append("\n");
			}
			DevLogger.printToDBLog("DB ERROR " + tmp + " for accession: " + acc.ACCESSION());
//			try {
//				e.printStackTrace(new PrintStream(new FileOutputStream("/Users/laptop/Desktop/errors/" + acc.ACCESSION() + ".txt")));
//			} catch (FileNotFoundException e1) {
//				//e1.printStackTrace();
//			}
			this.closeConnection();
		}
//		this.closeConnection();
	}
//	public void persistAccessionFile(Record acc, boolean bulk) {
////		this.openConnection();
////		String insertCompound = "INSERT INTO COMPOUND VALUES(?,?,?,?,?,?,?,?)";
////		PreparedStatement stmnt = con.prepareStatement(insertCompound);
//		
//		try {
//			String sql = "INSERT INTO CONTRIBUTOR (ACRONYM, SHORT_NAME, FULL_NAME) VALUES (NULL,?,NULL)";
//			PreparedStatement stmnt = con.prepareStatement(sql);
//			stmnt.setString(1, acc.CONTRIBUTOR());
//			stmnt.executeUpdate();
//		} catch (SQLException e1) {
////			 e1.printStackTrace();
//		}
//		
//		Integer conId = -1;
//		try {
//			String sql = "SELECT ID FROM CONTRIBUTOR WHERE SHORT_NAME = ?";
//			PreparedStatement stmnt = con.prepareStatement(sql);
//			stmnt.setString(1, acc.CONTRIBUTOR());
//			ResultSet res = stmnt.executeQuery();
//			if (res.next()) {
//				conId = res.getInt(1);
//			}
//		} catch (SQLException e1) {
//			e1.printStackTrace();
//		}
//		
//		try {
//		//System.out.println(System.nanoTime());
//		statementInsertCompound.setNull(1, java.sql.Types.INTEGER);
//		statementInsertCompound.setString(2, acc.get("CH$FORMULA").get(0)[2]);
//		statementInsertCompound.setString(3, acc.get("CH$EXACT_MASS").get(0)[2]);
//		statementInsertCompound.setString(4, acc.get("CH$SMILES").get(0)[2]);
//		statementInsertCompound.setString(5, acc.get("CH$IUPAC").get(0)[2]);
//		if (acc.get("CH$CDK_DEPICT_SMILES").size() != 0) {
//			statementInsertCompound.setString(6, acc.get("CH$CDK_DEPICT_SMILES").get(0)[2]);
//		} else {
//			statementInsertCompound.setNull(6, java.sql.Types.VARCHAR);
//		}
//		if (acc.get("CH$CDK_DEPICT_GENERIC_SMILES").size() != 0) {
//			statementInsertCompound.setString(7, acc.get("CH$CDK_DEPICT_GENERIC_SMILES").get(0)[2]);
//		} else {
//			statementInsertCompound.setNull(7, java.sql.Types.VARCHAR);
//		}
//		if (acc.get("CH$CDK_DEPICT_STRUCTURE_SMILES").size() != 0) {
//			statementInsertCompound.setString(8, acc.get("CH$CDK_DEPICT_STRUCTURE_SMILES").get(0)[2]);
//		} else {
//			statementInsertCompound.setNull(8, java.sql.Types.VARCHAR);
//		}
//		statementInsertCompound.executeUpdate();
//		ResultSet set = statementInsertCompound.getGeneratedKeys();
//		set.next();
//		int compoundId = set.getInt("ID");
//		
//		
//		//System.out.println(System.nanoTime());
//		int compoundClassId;
////		String insertCompoundClass = "INSERT INTO COMPOUND_CLASS VALUES(?,?,?,?)";
////		stmnt = con.prepareStatement(insertCompoundClass);
//		for (String[] el : acc.get("CH$COMPOUND_CLASS")) {
//			statementInsertCompound_Class.setNull(1, java.sql.Types.INTEGER);		
//			statementInsertCompound_Class.setString(2, null);
//			statementInsertCompound_Class.setString(3, null);
//			statementInsertCompound_Class.setString(4, el[2]);
//			statementInsertCompound_Class.executeUpdate();
//			set = statementInsertCompound_Class.getGeneratedKeys();
//			set.next();
//			compoundClassId = set.getInt("ID");
//			
////			String insertCompoundCompoundClass = "INSERT INTO COMPOUND_COMPOUND_CLASS VALUES(?,?)";
////			stmnt = con.prepareStatement(insertCompoundCompoundClass);
//			statementInsertCompound_Compound_Class.setInt(1, compoundId);
//			statementInsertCompound_Compound_Class.setInt(2, compoundClassId);
//			statementInsertCompound_Compound_Class.executeUpdate();
//		}
//		
//		//System.out.println(System.nanoTime());
//		int nameId;
////		String insertName = "INSERT INTO NAME VALUES(?,?)";
////		stmnt = con.prepareStatement(insertName);
//		for (String[] el : acc.get("CH$NAME")) {
//			statementInsertName.setNull(1, java.sql.Types.INTEGER);
//			statementInsertName.setString(2, el[2]);
//			try {
//				statementInsertName.executeUpdate();
//				set = statementInsertName.getGeneratedKeys();
//				set.next();
//				nameId = set.getInt("ID");
//
////				String insertCompoundName = "INSERT INTO COMPOUND_NAME VALUES(?,?)";
////				stmnt = con.prepareStatement(insertCompoundName);
//				statementInsertCompound_Name.setInt(1, compoundId);
//				statementInsertCompound_Name.setInt(2, nameId);
//				statementInsertCompound_Name.executeUpdate();
//			} catch (SQLException e) {
//				if (e.getErrorCode() == 1062) {
//					PreparedStatement retrieveIdForName = con.prepareStatement("SELECT ID FROM NAME WHERE CH_NAME = ?");
//					retrieveIdForName.setString(1, el[2]);
//					set = retrieveIdForName.executeQuery();
//					set.next();
//					nameId = set.getInt("ID");
//					statementInsertCompound_Name.setInt(1, compoundId);
//					statementInsertCompound_Name.setInt(2, nameId);
//					statementInsertCompound_Name.executeUpdate();
//				} else {
//					this.closeConnection();
//					throw e;
////					e.printStackTrace();
////					nameId = -1;
//				}
//			}
//		}
//		
//		//System.out.println(System.nanoTime());
////		String insertChLink = "INSERT INTO CH_LINK VALUES(?,?,?)";
////		stmnt = con.prepareStatement(insertChLink);
//		for (String[] el : acc.get("CH$LINK")) {
//			statementInsertCH_LINK.setInt(1,compoundId);
//			statementInsertCH_LINK.setString(2, el[1]);
//			statementInsertCH_LINK.setString(3, el[2]);
////			statementInsertCH_LINK.executeUpdate();
//			statementInsertCH_LINK.addBatch();
//		}
//		if (!bulk) {
//			statementInsertCH_LINK.executeBatch();
//		}
//		
//		//System.out.println(System.nanoTime());
//		int sampleId = -1;
//		statementInsertSAMPLE.setNull(1, java.sql.Types.INTEGER);
//		if (acc.get("SP$SCIENTIFIC_NAME").size() != 0) {
//			statementInsertSAMPLE.setString(2, acc.get("SP$SCIENTIFIC_NAME").get(0)[2]);
//		} else {
//			statementInsertSAMPLE.setNull(2, java.sql.Types.VARCHAR);
//		}
//		if (acc.get("SP$LINEAGE").size() != 0) {
//			statementInsertSAMPLE.setString(3, acc.get("SP$LINEAGE").get(0)[2]);
//		} else {
//			statementInsertSAMPLE.setNull(3, java.sql.Types.VARCHAR);
//		}
//		if (acc.get("SP$SCIENTIFIC_NAME").size() != 0 && acc.get("SP$LINEAGE").size() != 0) {
//			statementInsertSAMPLE.executeUpdate();
//			set = statementInsertSAMPLE.getGeneratedKeys();
//			set.next();
//			sampleId = set.getInt("ID");
//		}
//		
//		//System.out.println(System.nanoTime());
//		for (String[] el : acc.get("SP$LINK")) {
//			statementInsertSP_LINK.setInt(1, sampleId);
//			statementInsertSP_LINK.setString(2, el[2]);
////			statementInsertSP_LINK.executeUpdate();
//			statementInsertSP_LINK.addBatch();
//		}
//		if (!bulk) {
//			statementInsertSP_LINK.executeBatch();
//		}
//		
//		//System.out.println(System.nanoTime());
//		for (String[] el : acc.get("SP$SAMPLE")) {
//			statementInsertSP_SAMPLE.setInt(1, sampleId);
//			statementInsertSP_SAMPLE.setString(2, el[2]);
////			statementInsertSP_SAMPLE.executeUpdate();
//			statementInsertSP_SAMPLE.addBatch();
//		}
//		if (!bulk) {
//			statementInsertSP_SAMPLE.executeBatch();
//		}
//		
//		//System.out.println(System.nanoTime());
//		statementInsertINSTRUMENT.setNull(1, java.sql.Types.INTEGER);
//		statementInsertINSTRUMENT.setString(2, acc.get("AC$INSTRUMENT").get(0)[2]);
//		statementInsertINSTRUMENT.setString(3, acc.get("AC$INSTRUMENT_TYPE").get(0)[2]);
//		statementInsertINSTRUMENT.executeUpdate();
//		set = statementInsertINSTRUMENT.getGeneratedKeys();
//		set.next();
//		int instrumentId = set.getInt("ID");
//		
//		//System.out.println(System.nanoTime());
//		statementInsertRECORD.setString(1, acc.get("ACCESSION").get(0)[2]);
//		statementInsertRECORD.setString(2, acc.get("RECORD_TITLE").get(0)[2]);
//		statementInsertRECORD.setString(3, acc.get("DATE").get(0)[2]);
//		statementInsertRECORD.setString(4, acc.get("AUTHORS").get(0)[2]);
//		if (acc.get("LICENSE").size() != 0) {
//			statementInsertRECORD.setString(5, acc.get("LICENSE").get(0)[2]);			
//		} else {
//			statementInsertRECORD.setNull(5, java.sql.Types.VARCHAR);
//		}
//		if (acc.get("COPYRIGHT").size() != 0) {
//			statementInsertRECORD.setString(6, acc.get("COPYRIGHT").get(0)[2]);			
//		} else {
//			statementInsertRECORD.setNull(6, java.sql.Types.VARCHAR);
//		}
//		if (acc.get("PUBLICATION").size() != 0) {
//			statementInsertRECORD.setString(7, acc.get("PUBLICATION").get(0)[2]);			
//		} else {
//			statementInsertRECORD.setNull(7, java.sql.Types.VARCHAR);
//		}
//		statementInsertRECORD.setInt(8, compoundId);
//		if (sampleId > 0) {
//			statementInsertRECORD.setInt(9, sampleId);
//		} else {
//			statementInsertRECORD.setNull(9, java.sql.Types.INTEGER);
//		}
//		statementInsertRECORD.setInt(10, instrumentId);
//		statementInsertRECORD.setString(11, acc.get("AC$MASS_SPECTROMETRY", "MS_TYPE").get(0)[2]);
//		statementInsertRECORD.setString(12, acc.get("AC$MASS_SPECTROMETRY", "ION_MODE").get(0)[2]);
//		statementInsertRECORD.setString(13, acc.get("PK$SPLASH").get(0)[2]);
//		statementInsertRECORD.setInt(14, conId);
//		statementInsertRECORD.executeUpdate();
////		set = statementInsertRECORD.getGeneratedKeys();
////		set.next();
//		String accession = acc.get("ACCESSION").get(0)[2];
//		
//		//System.out.println(System.nanoTime());
//		for (String[] el : acc.get("COMMENT")) {
//			statementInsertCOMMENT.setString(1, accession);
//			statementInsertCOMMENT.setString(2, el[2]);
////			statementInsertCOMMENT.executeUpdate();
//			statementInsertCOMMENT.addBatch();
//		}
//		if (!bulk) {
//			statementInsertCOMMENT.executeBatch();
//		}
//		
//		//System.out.println(System.nanoTime());
//		for (String[] el : acc.get("AC$MASS_SPECTROMETRY")) {
//			statementInsertAC_MASS_SPECTROMETRY.setString(1, accession);
//			statementInsertAC_MASS_SPECTROMETRY.setString(2, el[1]);
//			statementInsertAC_MASS_SPECTROMETRY.setString(3, el[2]);
////			statementInsertAC_MASS_SPECTROMETRY.executeUpdate();
//			statementInsertAC_MASS_SPECTROMETRY.addBatch();
//		}
//		if (!bulk) {
//			statementInsertAC_MASS_SPECTROMETRY.executeBatch();
//		}
//		
//		//System.out.println(System.nanoTime());
//		for (String[] el : acc.get("AC$CHROMATOGRAPHY")) {
//			statementInsertAC_CHROMATOGRAPHY.setString(1, accession);
//			statementInsertAC_CHROMATOGRAPHY.setString(2, el[1]);
//			statementInsertAC_CHROMATOGRAPHY.setString(3, el[2]);
////			statementInsertAC_CHROMATOGRAPHY.executeUpdate();
//			statementInsertAC_CHROMATOGRAPHY.addBatch();
//		}
//		if (!bulk) {
//			statementInsertAC_CHROMATOGRAPHY.executeBatch();
//		}
//		
//		//System.out.println(System.nanoTime());
//		for (String[] el : acc.get("MS$FOCUSED_ION")) {
//			statementInsertMS_FOCUSED_ION.setString(1, accession);
//			statementInsertMS_FOCUSED_ION.setString(2, el[1]);
//			statementInsertMS_FOCUSED_ION.setString(3, el[2]);
////			statementInsertMS_FOCUSED_ION.executeUpdate();
//			statementInsertMS_FOCUSED_ION.addBatch();
//		}
//		if (!bulk) {
//			statementInsertMS_FOCUSED_ION.executeBatch();
//		}
//		
//		//System.out.println(System.nanoTime());
//		for (String[] el : acc.get("MS$DATA_PROCESSING")) {
//			statementInsertMS_DATA_PROCESSING.setString(1, accession);
//			statementInsertMS_DATA_PROCESSING.setString(2, el[1]);
//			statementInsertMS_DATA_PROCESSING.setString(3, el[2]);
////			statementInsertMS_DATA_PROCESSING.executeUpdate();
//			statementInsertMS_DATA_PROCESSING.addBatch();
//		}
//		if (!bulk) {
//			statementInsertMS_DATA_PROCESSING.executeBatch();
//		}
//
//		//System.out.println(System.nanoTime());
//		ArrayList<String[]> peak = acc.get("PK$PEAK");
//		for (int i = 1; i < peak.size(); i++) {
//			statementInsertPEAK.setString(1, accession);
//			String values = peak.get(i)[2];
//			statementInsertPEAK.setDouble(2, Double.parseDouble(values.substring(0, values.indexOf(" "))));
//			values = values.substring(values.indexOf(" ")+1, values.length());
//			statementInsertPEAK.setFloat(3, Float.parseFloat(values.substring(0, values.indexOf(" "))));
//			values = values.substring(values.indexOf(" ")+1, values.length());
//			statementInsertPEAK.setShort(4, Short.parseShort(values.substring(0, values.length())));
//			statementInsertPEAK.setNull(5, java.sql.Types.VARCHAR);
////			statementInsertPEAK.setNull(5, java.sql.Types.VARCHAR);
////			statementInsertPEAK.setNull(6, java.sql.Types.SMALLINT);
////			statementInsertPEAK.setNull(7, java.sql.Types.FLOAT);
////			statementInsertPEAK.setNull(8, java.sql.Types.FLOAT);
////			statementInsertPEAK.executeUpdate();
//			statementInsertPEAK.addBatch();
//		}
//		if (!bulk) {
//			statementInsertPEAK.executeBatch();
//		}
//		
//		//System.out.println(System.nanoTime());
//		ArrayList<String[]> annotation = acc.get("PK$ANNOTATION");
//		if (annotation.size() != 0) {
//			statementInsertANNOTATION_HEADER.setString(1, accession);
//			statementInsertANNOTATION_HEADER.setString(2, annotation.get(0)[2]);
//			statementInsertANNOTATION_HEADER.executeUpdate();
//		}
//		for (int i = 1; i < annotation.size(); i++) {
//			String values = annotation.get(i)[2];
//			Float mz = Float.parseFloat(values.substring(0, values.indexOf(" ")));
////			values = values.substring(values.indexOf(" ")+1, values.length());
//			statementUpdatePEAKs.setString(1, values);
//			statementUpdatePEAKs.setString(2, accession);
//			statementUpdatePEAKs.setFloat(3, mz);
////			statementUpdatePEAK.setString(1, values.substring(0, values.indexOf(" ")));
////			values = values.substring(values.indexOf(" ")+1, values.length());
////			statementUpdatePEAK.setShort(2, Short.parseShort(values.substring(0, values.indexOf(" "))));
////			values = values.substring(values.indexOf(" ")+1, values.length());
////			statementUpdatePEAK.setFloat(3, Float.parseFloat(values.substring(0, values.indexOf(" "))));
////			values = values.substring(values.indexOf(" ")+1, values.length());
////			statementUpdatePEAK.setFloat(4, Float.parseFloat(values.substring(0, values.length())));
////			statementUpdatePEAK.setString(5, accession);
////			statementUpdatePEAK.setFloat(6, mz);
////			statementUpdatePEAK.executeUpdate();
//			statementUpdatePEAKs.addBatch();
//		}
//		if (!bulk) {
//			statementUpdatePEAKs.executeBatch();
//		}
//		
//		//System.out.println(System.nanoTime());
//		con.commit();
//		//System.out.println(System.nanoTime());
//		
//		} catch (SQLException e) {
//			StringBuilder tmp = new StringBuilder();
//			tmp.append(e.getMessage());
//			tmp.append("\n");
//			for (StackTraceElement el : e.getStackTrace()) {
//				tmp.append(el.toString());
//				tmp.append("\n");
//			}
//			DevLogger.printToDBLog("DB ERROR " + tmp + " for accession: " + acc.get("ACCESSION").get(0)[2]);
////			try {
////				e.printStackTrace(new PrintStream(new FileOutputStream("/Users/laptop/Desktop/errors/" + acc.get("ACCESSION").get(0)[2] + ".txt")));
////			} catch (FileNotFoundException e1) {
////				//e1.printStackTrace();
////			}
//			this.closeConnection();
//		} catch (IndexOutOfBoundsException e) {
//			StringBuilder tmp = new StringBuilder();
//			tmp.append(e.getMessage());
//			tmp.append("\n");
//			for (StackTraceElement el : e.getStackTrace()) {
//				tmp.append(el.toString());
//				tmp.append("\n");
//			}
//			DevLogger.printToDBLog("DB ERROR " + tmp + " for accession: " + acc.get("ACCESSION").get(0)[2]);
////			System.out.println(acc.get("ACCESSION").get(0)[2]);
////			System.out.println(acc.get("PK$PEAK").size());
////			System.out.println(acc.get("PK$ANNOTATION").size());
////			try {
////				e.printStackTrace(new PrintStream(new FileOutputStream("/Users/laptop/Desktop/errors/" + acc.get("ACCESSION").get(0)[2] + ".txt")));
////			} catch (FileNotFoundException e1) {
////				//e1.printStackTrace();
////			}
//		} catch (Exception e) {
//			StringBuilder tmp = new StringBuilder();
//			tmp.append(e.getMessage());
//			tmp.append("\n");
//			for (StackTraceElement el : e.getStackTrace()) {
//				tmp.append(el.toString());
//				tmp.append("\n");
//			}
//			DevLogger.printToDBLog("DB ERROR " + tmp + " for accession: " + acc.get("ACCESSION").get(0)[2]);
////			try {
////				e.printStackTrace(new PrintStream(new FileOutputStream("/Users/laptop/Desktop/errors/" + acc.get("ACCESSION").get(0)[2] + ".txt")));
////			} catch (FileNotFoundException e1) {
////				//e1.printStackTrace();
////			}
//			this.closeConnection();
//		}
//		this.closeConnection();
//	}

	public Record.Contributor getContributorFromAccession(String accessionId) throws SQLException {
//		String accessionId	= "OUF01001";
		this.statementGetContributorFromAccession.setString(1, accessionId);
		ResultSet tmp = this.statementGetContributorFromAccession.executeQuery();
		
		if (!tmp.next()) throw new IllegalStateException("Accession '" + accessionId + "' is not in database");
		
		// CONTRIBUTOR.ACRONYM, CONTRIBUTOR.SHORT_NAME, CONTRIBUTOR.FULL_NAME
//		System.out.println(tmp.getString("CONTRIBUTOR.ACRONYM"));
//		System.out.println(tmp.getString("CONTRIBUTOR.SHORT_NAME"));
//		System.out.println(tmp.getString("CONTRIBUTOR.FULL_NAME"));
		return new Record.Contributor(
				tmp.getString("CONTRIBUTOR.ACRONYM"), 
				tmp.getString("CONTRIBUTOR.SHORT_NAME"), 
				tmp.getString("CONTRIBUTOR.FULL_NAME")
		);
	}	

	public static void main (String[] args) throws SQLException {

	}
}
