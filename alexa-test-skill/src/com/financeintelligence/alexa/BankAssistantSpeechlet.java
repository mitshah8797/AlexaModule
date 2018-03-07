package com.financeintelligence.alexa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;

public class BankAssistantSpeechlet implements SpeechletV2 {
	private static final Logger log = LoggerFactory.getLogger(BankAssistantSpeechlet.class);

	private static final String LOAN_TYPE_SLOT = "LoanName";
	private static final String EMI_AMOUNT_SLOT = "Amount";

	private static final String LOAN_TYPE = "LOANTYPE";
	private static final String LOAN_AMOUNT = "LOANAMOUNT";
	private static final String LOAN_PERIOD = "LOANPERIOD";

	private static boolean emiFlag = false;
	private static int emiStatus = 0;

	@Override
	public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
		log.info("onSessionStarted requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
				requestEnvelope.getSession().getSessionId());
		// logging the first activity.
	}

	@Override
	public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
		log.info("onLaunch requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
				requestEnvelope.getSession().getSessionId());
		return getWelcomeResponse();
	}

	@Override
	public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
		IntentRequest request = requestEnvelope.getRequest();
		Session session = requestEnvelope.getSession();
		log.info("onIntent requestId={}, sessionId={}", request.getRequestId(), session);

		// Get intent from the request object.
		Intent intent = request.getIntent();
		String intentName = (intent != null) ? intent.getName() : null;

		// Note: If the session is started with an intent, no welcome message will be
		// rendered;
		// rather, the intent specific response will be returned.

		if (!emiFlag) {
			if ("GreetingIntent".equals(intentName)) {
				return getGreetingResponce();
			} else if ("ListLoanIntent".equals(intentName)) {
				try {
					return getLoanTypeResponce();
				} catch (SQLException e) {
					String errorSpeech = "This is unsupported.  Please try something else.";
					return getSpeechletResponse(errorSpeech, errorSpeech, true);
				}
			} else if ("SessionEndIntent".equals(intentName)) {
				return getEndSessionResponse();
			} else if ("LoanInfoIntent".equals(intentName)) {
				return getLoanDetailsResponce(intent, session);
			} else if ("LoanInterestIntent".equals(intentName)) {
				return getLoanInterestDetailsResponce(intent, session, false);
			} else if ("LoanInterestInSessionIntent".equals(intentName)) {
				return getLoanInterestDetailsResponce(intent, session, true);
			} else if ("LoanDocumentIntent".equals(intentName)) {
				return getLoanDocumentDetailsResponce(intent, session, false);
			} else if ("LoanEMIIntent".equals(intentName)) {
				return getEMIResponce(intent, session);
			} else if ("AMAZON.HelpIntent".equals(intentName)) {
				return getGreetingResponce();
			} else {
				String errorSpeech = "This is unsupported.  Please try something else.";
				return getSpeechletResponse(errorSpeech, errorSpeech, true);
			}
		} else {
			if ("LoanEMIInquieryIntent".equals(intentName)) {
				return getEMIResponce(intent, session);
			} else {
				String errorSpeech = "This is unsupported.  Please try something else.";
				return getSpeechletResponse(errorSpeech, errorSpeech, true);
			}
		}
	}

	@Override
	public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
		// TODO Auto-generated method stub

	}

	private SpeechletResponse getWelcomeResponse() {
		// Create the welcome message.
		String speechText = "Welcome to the Dummy Bank. You can say Hello!";
		String repromptText = "You just have to say Hello";

		return getSpeechletResponse(speechText, repromptText, true);
	}

	private SpeechletResponse getGreetingResponce() {
		String speechText = "Hello there. I am your bank assistant for the session. "
				+ "You can ask me about any loan regarding queries. Just say Loan!";
		String repromptText = "You just have to say Loan.";

		return getSpeechletResponse(speechText, repromptText, true);
	}

	private SpeechletResponse getLoanTypeResponce() throws SQLException {
		ResultSet loanTypes = DatabaseOperations.getAllLoanTypes();
		String speechText = "We have different types of loans. Few are mentioned here. ";
		while (loanTypes.next()) {
			speechText += loanTypes.getString("TypeName");
			speechText += ", ";
		}
		speechText += ". Which type of loan do you want to know about?";
		String repromptText = "You just have to say the loan name.";

		return getSpeechletResponse(speechText, repromptText, true);
	}

	private SpeechletResponse getLoanDetailsResponce(final Intent intent, final Session session) {
		// Get the slots from the intent.
		Map<String, Slot> slots = intent.getSlots();

		Slot loanTypeSlot = slots.get(LOAN_TYPE_SLOT);

		String speechText;
		String repromptText = "May I help you?";

		if (loanTypeSlot != null) {
			String loanType = loanTypeSlot.getValue();
			speechText = DatabaseOperations.getLoanDescription(loanType);
			session.setAttribute(LOAN_TYPE, loanType);
		} else {
			speechText = "Please tell me the specific loan type you want to know about.";
		}
		return getSpeechletResponse(speechText, repromptText, true);
	}

	private SpeechletResponse getLoanInterestDetailsResponce(final Intent intent, final Session session,
			boolean inSession) {
		// Get the slots from the intent.

		Slot loanTypeSlot = intent.getSlot(LOAN_TYPE_SLOT);

		String speechText;
		String repromptText = "May I help you?";
		if (inSession) {
			if (session.getAttribute(LOAN_TYPE) != null) {
				String loanType = (String) session.getAttribute(LOAN_TYPE);
				speechText = DatabaseOperations.getLoanInterestDescription(loanType);
			} else {
				speechText = "Please tell me the specific loan type you want to know about.";
			}
		} else {
			if (loanTypeSlot != null) {
				String loanType = loanTypeSlot.getValue();
				speechText = DatabaseOperations.getLoanInterestDescription(loanType);
				session.setAttribute(LOAN_TYPE, loanType);
			} else {
				speechText = "Please tell me the specific loan type you want to know about.";
			}
		}
		return getSpeechletResponse(speechText, repromptText, true);
	}

	private SpeechletResponse getLoanDocumentDetailsResponce(final Intent intent, final Session session,
			boolean inSession) {
		// Get the slots from the intent.

		Slot loanTypeSlot = intent.getSlot(LOAN_TYPE_SLOT);

		String speechText;
		String repromptText = "May I help you?";
		if (inSession) {
			if (session.getAttribute(LOAN_TYPE) != null) {
				String loanType = (String) session.getAttribute(LOAN_TYPE);
				speechText = DatabaseOperations.getLoanDocumentDescription(loanType);
			} else {
				speechText = "Please tell me the specific loan type you want to know about.";
			}
		} else {
			if (loanTypeSlot != null) {
				String loanType = loanTypeSlot.getValue();
				speechText = DatabaseOperations.getLoanDocumentDescription(loanType);
				session.setAttribute(LOAN_TYPE, loanType);
			} else {
				speechText = "Please tell me the specific loan type you want to know about.";
			}
		}
		return getSpeechletResponse(speechText, repromptText, true);
	}

	private SpeechletResponse getEMIResponce(final Intent intent, final Session session) {
		// Get the slots from the intent.
		String speechText = null;
		String repromptText = "May I help you?";

		if (emiStatus == 0) {
			Slot loanTypeSlot = intent.getSlot(LOAN_TYPE_SLOT);
			if (loanTypeSlot != null) {
				String loanType = loanTypeSlot.getValue();
				session.setAttribute(LOAN_TYPE, loanType);
				speechText = "Can you tell me the amount of loan you need?";
				emiStatus = 1;
				emiFlag = true;
			} else {
				speechText = "Please tell me the specific loan type you want to know about.";
			}
		} else if (emiStatus == 1) {
			Slot loanAmountSlot = intent.getSlot(EMI_AMOUNT_SLOT);
			if (loanAmountSlot != null) {
				double loanAmount = Double.parseDouble(loanAmountSlot.getValue());
				session.setAttribute(LOAN_AMOUNT, loanAmount);
				speechText = "Can you tell me the period of loan in months?";
				emiStatus = 2;
			} else {
				speechText = "Please tell me the loan amount.";
			}

		} else if (emiStatus == 2) {
			Slot loanPeriodSlot = intent.getSlot(EMI_AMOUNT_SLOT);
			if (loanPeriodSlot != null) {
				double loanPeriod = Double.parseDouble(loanPeriodSlot.getValue());
				session.setAttribute(LOAN_PERIOD, loanPeriod);
				speechText = "Can you tell me the period of loan in months?";
				emiStatus = 3;
			} else {
				speechText = "Please tell me the loan period.";
			}

		} else if (emiStatus == 2) {
			Slot loanDownpaymentSlot = intent.getSlot(EMI_AMOUNT_SLOT);
			if (loanDownpaymentSlot != null) {
				String loanName = (String) session.getAttribute(LOAN_TYPE);
				double loanDownpayment = Double.parseDouble(loanDownpaymentSlot.getValue());
				double loanPeriod = (double) session.getAttribute(LOAN_PERIOD);
				double loanAmount = (double) session.getAttribute(LOAN_AMOUNT);

				double interestRate = DatabaseOperations.getLoanInterestRate(loanName);

				interestRate = interestRate / (12 * 100);
				double principalAmount = loanAmount - loanDownpayment;
				double temp = Math.pow(interestRate + 1, loanPeriod);
				double emi = (principalAmount * interestRate * temp) / (temp - 1);

				speechText = "Your EMI for " + loanName + " for " + loanAmount + " rupees at " + interestRate
						+ "% interest rate with " + loanDownpayment + " rupees downpayment for " + loanPeriod
						+ " months is " + emi;
				emiStatus = 0;
				emiFlag = false;
			} else {
				speechText = "Please tell me the loan period.";
			}

		}
		return getSpeechletResponse(speechText, repromptText, true);
	}

	private SpeechletResponse getEndSessionResponse() {
		String speechText = "Ok. Thank you for chatting with me. Have a nice day!";

		return getSpeechletResponse(speechText, speechText, false);
	}

	private SpeechletResponse getSpeechletResponse(String speechText, String repromptText, boolean isAskResponse) {
		// Create the Simple card content.
		SimpleCard card = new SimpleCard();
		card.setTitle("Bank Assistant");
		card.setContent(speechText);

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);

		if (isAskResponse) {
			// Create reprompt
			PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
			repromptSpeech.setText(repromptText);
			Reprompt reprompt = new Reprompt();
			reprompt.setOutputSpeech(repromptSpeech);

			return SpeechletResponse.newAskResponse(speech, reprompt, card);

		} else {
			return SpeechletResponse.newTellResponse(speech, card);
		}
	}
}
