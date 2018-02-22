package com.financeintelligence.alexa;

import java.util.HashSet;
import java.util.Set;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;

/*
 * Add Handler name as com.financeintelligence.alexa.BankAssistantSpeechletRequestHandler 
 * in Lambda function!!
*/

public class BankAssistantSpeechletRequestHandler extends SpeechletRequestStreamHandler {
	private static final Set<String> supportedApplicationIds;
    
    static {
        /*
         * This Id can be found on https://developer.amazon.com/edw/home.html#/ "Edit" the relevant
         * Alexa Skill and put the relevant Application Ids in this Set.
         */
        supportedApplicationIds = new HashSet<String>();
        // supportedApplicationIds.add("[unique-value-here]");
    }
    
    public BankAssistantSpeechletRequestHandler() {
		super(new BankAssistantSpeechlet(), supportedApplicationIds);
		// TODO Auto-generated constructor stub
	}
}
