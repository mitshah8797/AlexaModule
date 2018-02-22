package com.financeintelligence.alexa;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
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
		if ("GreetingIntent".equals(intentName)) {
			return getGreetingResponce();
		} else if ("ListLoanIntent".equals(intentName)) {
			try {
				return getLoanTypeResponce();
			} catch (SQLException e) {
				String errorSpeech = "This is unsupported.  Please try something else.";
				return getSpeechletResponse(errorSpeech, errorSpeech, true);
			}
		} else if ("LoanDetailIntent".equals(intentName)) {
			return getLoanDetailsResponce();
		} else if ("SessionEndIntent".equals(intentName)) {
			return getEndSessionResponse();
		} else if ("AMAZON.HelpIntent".equals(intentName)) {
			return getGreetingResponce();
		} else {
			String errorSpeech = "This is unsupported.  Please try something else.";
			return getSpeechletResponse(errorSpeech, errorSpeech, true);
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
	
	private SpeechletResponse getLoanDetailsResponce() {
		String speechText = "We have 'Apna Ghar' scheme going on for housing loan."
				+ "The interest rate is very low at 9.5%. It is usaually a 10 year or more scheme "
				+ "depending on the amount you need as a loan. Maximum 90% loan can be "
				+ "sacnctioned! I hope you have all the details you need. May I assist you any further?";
		String repromptText = "May I help you?";

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
