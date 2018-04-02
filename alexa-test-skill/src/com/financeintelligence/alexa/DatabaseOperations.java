package com.financeintelligence.alexa;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

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

			ResultSet rs = st.executeQuery("SELECT * FROM LoanTypeTable WHERE TypeName LIKE '" + loanName + "%'");
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

			ResultSet rs = st.executeQuery("SELECT * FROM LoanTypeTable WHERE TypeName LIKE '" + loanName + "%'");
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

			ResultSet rs = st.executeQuery("SELECT * FROM LoanTypeTable WHERE TypeName LIKE '" + loanName + "%'");
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

			ResultSet rs = st.executeQuery("SELECT DocumentTypeTable.DocumentType as type "
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

	public static String addUserByPhoneNumber(String phoneNumber, double amount, double period, double downpayment,
			String loanType) {
		String speechText = null;
		Savepoint sp = null;
		int loanTypeId = 0, userId = 0;
		try {
			connection.setAutoCommit(false);
			sp = connection.setSavepoint();
			Statement st = connection.createStatement();

			String sql = "INSERT INTO LoginTable (Email, Password, Role, Enabled) VALUES (?,?,?,?);";
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			String userName = createUserName();
			preparedStatement.setString(1, userName);
			preparedStatement.setString(2, createPassword());
			preparedStatement.setString(3, "ROLE_CUSTOMER");
			preparedStatement.setString(4, "1");

			preparedStatement.executeUpdate();

			ResultSet rs = st.executeQuery("SELECT * FROM LoginTable WHERE Email = '" + userName + "'");
			int loginId = 0;
			if (rs.next()) {
				loginId = rs.getInt("loginId");
			}

			if (loginId == 0) {
				speechText = "We are unable to find your account and/or loan type. Sorry for inconvience. Please come back later!!";
				return speechText;
			}

			sql = "INSERT INTO UserTable (UserMobileNo, login_loginId) VALUES (?,?);";
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setString(1, phoneNumber);
			preparedStatement.setInt(2, loginId);

			preparedStatement.executeUpdate();

			rs = st.executeQuery("SELECT * FROM UserTable WHERE login_loginId = " + loginId);

			if (rs.next()) {
				userId = rs.getInt("userId");
			}

			rs = st.executeQuery("SELECT * FROM LoanTypeTable WHERE TypeName LIKE '" + loanType + "%'");
			if (rs.next()) {
				loanTypeId = rs.getInt("loanTypeId");
			}

			if (userId == 0 || loanTypeId == 0) {
				speechText = "We are unable to find your account and/or loan type. Sorry for inconvience. Please come back later!!";
				return speechText;
			}

			sql = "INSERT INTO LoanApplicationTable (LoanAmount, LoanPeriod, Status, customer_userId, loanType_loanTypeId, LoanDownpayment) VALUES (?,?,?,?,?,?);";
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setDouble(1, amount);
			preparedStatement.setInt(2, (int) period);
			preparedStatement.setString(3, "STATUS_PENDING");
			preparedStatement.setInt(4, userId);
			preparedStatement.setInt(5, loanTypeId);
			preparedStatement.setDouble(6, downpayment);

			int update = preparedStatement.executeUpdate();

			speechText = "Okay. I have got your mobile number. I'll send all the details regarding your "
					+ "loan application via sms on your registered mobile number. Please confirm your details "
					+ "and upload required documents. Have a nice day!!";

			connection.commit();
			return speechText;
		} catch (Exception e) {
			speechText = "Sorry, there is a problem in Loan Application. Sorry for your inconvience. Please come later. Have a nice day!!";
			e.printStackTrace();
			try {
				connection.rollback(sp);
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return speechText;
		}
	}

	public static String addUserByAccountNumber(String accountNumber, double amount, double period, double downpayment,
			String loanType) {
		String speechText = null;
		int loanTypeId = 0, userId = 0;
		try {
			Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery("SELECT * FROM UserTable WHERE AccountNumber = " + accountNumber);
			if (rs.next()) {
				userId = rs.getInt("userId");
			}

			rs = st.executeQuery("SELECT * FROM LoanTypeTable WHERE TypeName LIKE '" + loanType + "%'");
			if (rs.next()) {
				loanTypeId = rs.getInt("loanTypeId");
			}

			if (userId == 0 || loanTypeId == 0) {
				speechText = "We are unable to find your account and/or loan type. Sorry for inconvience. Please come back later!!";
				return speechText;
			}

			String sql = "INSERT INTO LoanApplicationTable (LoanAmount, LoanPeriod, Status, customer_userId, loanType_loanTypeId, LoanDownpayment) VALUES (?,?,?,?,?,?);";
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setDouble(1, amount);
			preparedStatement.setInt(2, (int) period);
			preparedStatement.setString(3, "STATUS_PENDING");
			preparedStatement.setInt(4, userId);
			preparedStatement.setInt(5, loanTypeId);
			preparedStatement.setDouble(6, downpayment);

			int update = preparedStatement.executeUpdate();

			speechText = "Okay. I have got your account number. I'll send all the details regarding your "
					+ "loan application via sms/email on your registered mobile number/email id. Please confirm your details "
					+ "and upload required documents. Have a nice day!!";
			return speechText;
		} catch (Exception e) {
			speechText = "Sorry, there is a problem in Loan Application. Sorry for your inconvience. Please come later. Have a nice day!!";
			e.printStackTrace();
			return speechText;
		}
	}

	private static String createUserName() {
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy@hhmmss");
		String strDate = formatter.format(date);
		String userName = "tempuser" + strDate;
		return userName;
	}

	private static String createPassword() {
		String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		StringBuilder builder = new StringBuilder();
		int count = 8;
		while (count-- != 0) {
			int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
			builder.append(ALPHA_NUMERIC_STRING.charAt(character));
		}

		return builder.toString();
	}
}