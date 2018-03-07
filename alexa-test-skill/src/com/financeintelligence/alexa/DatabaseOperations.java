package com.financeintelligence.alexa;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseOperations {
	private static final String DB_URL = "jdbc:mysql://myprojectdbinstanc.cerzsvq485c8.us-east-1.rds.amazonaws.com:3306";
	private static final String DB_DATABASE = "myprojectdb";
	private static final String DB_USER = "mitshah";
	private static final String DB_PASS = "SmH8797!!";

	private static Connection connection = null;

	static {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection(DB_URL + "/" + DB_DATABASE, DB_USER, DB_PASS);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ResultSet getAllLoanTypes() {
		try {
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery("SELECT * FROM LoanTypeTable");
			return rs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getLoanDescription(String loanName) {
		String response = null;
		try {
			Statement st = connection.createStatement();

			ResultSet rs = st.executeQuery("SELECT * FROM SynonymsTable WHERE Synonyms LIKE '" + loanName + "%'");
			if (rs.next()) {
				loanName = rs.getString("SlotName");
			}

			rs = st.executeQuery("SELECT * FROM LoanTypeTable WHERE TypeName LIKE '" + loanName + "%'");
			if (rs.next()) {
				response = rs.getString("Description");
			} else {
				response = loanName;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	public static String getLoanInterestDescription(String loanName) {
		String response = null;
		try {
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery("SELECT * FROM SynonymsTable WHERE Synonyms LIKE '" + loanName + "%'");
			if (rs.next()) {
				loanName = rs.getString("SlotName");
			}

			rs = st.executeQuery("SELECT * FROM LoanTypeTable WHERE TypeName LIKE '" + loanName + "%'");
			if (rs.next()) {
				response = "Interest rate for " + loanName + " loan is " + rs.getDouble("Interest") + ".";
			} else {
				response = loanName;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	public static double getLoanInterestRate(String loanName) {
		double interestRate = 0.0;
		try {
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery("SELECT * FROM SynonymsTable WHERE Synonyms LIKE '" + loanName + "%'");
			if (rs.next()) {
				loanName = rs.getString("SlotName");
			}

			rs = st.executeQuery("SELECT * FROM LoanTypeTable WHERE TypeName LIKE '" + loanName + "%'");
			if (rs.next()) {
				interestRate = rs.getDouble("Interest");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return interestRate;
	}

	public static String getLoanDocumentDescription(String loanName) {
		String response = null;
		try {
			Statement st = connection.createStatement();

			ResultSet rs = st
					.executeQuery("SELECT SlotName FROM SynonymsTable WHERE Synonyms LIKE '" + loanName + "%'");
			if (rs.next()) {
				loanName = rs.getString("SlotName");
			}

			rs = st.executeQuery("SELECT DocumentTypeTable.DocumentType as type "
					+ "FROM DocumentTypeTable INNER JOIN DocumentMappingTable INNER JOIN LoanTypeTable "
					+ "ON DocumentTypeTable.documentTypeId = DocumentMappingTable.docType_documentTypeId "
					+ "AND LoanTypeTable.loanTypeId = DocumentMappingTable.loanType_loanTypeId "
					+ "WHERE LoanTypeTable.TypeName LIKE '" + loanName + "%'");
			response = "Required documents for " + loanName + " loan are following :\n";
			while (rs.next()) {
				response += rs.getString("type") + ", ";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}
}