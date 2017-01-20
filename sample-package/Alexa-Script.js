var request = require('request');
var firebase = require('firebase');
var config = {
    apiKey: "my-api-key",
    authDomain: "stuff",
    databaseURL: "more-stuff",
    storageBucket: "even-more-stuff"
};
firebase.initializeApp(config);
// Get a reference to the database
var rootRef = firebase.database();
var userId;

exports.handler = (event, context) => {
    try {
        userId = event.session.user.userId;
        if (event.session.new) {
            // New Session
            console.log("NEW SESSION");
        }
        // Launch Request
        switch (event.request.type) {
            // Intent Request
            case "IntentRequest":
                console.log("INTENT REQUEST");
                switch (event.request.intent.name) {
                    case "TestIntent":
                        console.log("TestIntent");
                        var intentRef = makeIntentRef();
                        test1("TestIntent", intentRef, context);
                        break;
                    default:
                        console.log("IntentRequest Default");
                        break;
                }
                break;
            // Session Ended Request
            case "SessionEndedRequest":
                console.log("SESSION ENDED REQUEST");
                break;

            default:
                context.fail(`INVALID REQUEST TYPE: ${event.request.type}`);
        }
    }
    catch (error) {
        context.fail(`Exception: ${error}`);
    }

}
    // Helpers
buildSpeechletReponse = (outputText, shouldEndSession) => {
    return {
        outputSpeech : {
            type: "PlainText",
            text: outputText
        },
        shouldEndSession: shouldEndSession
    };
}

generateResponse = (speechletResponse, sessionAttributes) => {
    return {
        version: "1.0",
        sessionAttributes: sessionAttributes,
        response: speechletResponse
    };
}


function makeIntentRef(){
    return rootRef.ref("users/" + processUserId() + "/intentQueue/" + new Date().getTime());
}


function test1(intentName, targetRef, context) {
    console.log("writing");
    targetRef.set({
        intent: intentName,
        done: false
    }).then(function() {
        return targetRef.orderByChild("done").equalTo(true).on("value");
    }).then(function(snapshot) {
        var res = snapshot.val().result;
        console.log("Res: " + res);
        context.succeed( //context.succeed should be called after "result" has a value.
            generateResponse(
                buildSpeechletReponse("The result is" + processNumbersForSpeech(res), true),
                {}
            )
        );
    });
}


function processNumbersForSpeech(pin) {
    var wordNumArr = ["zero", "one", "two", "three", "four",
     "five", "six", "seven", "eight", "nine"];
    processedPin = "";
    for (i = 0; i < pin.length; i++){
        var currentChar = pin.charAt(i);
        if (isNaN(Number(currentChar))){
            processedPin += currentChar + ". ";
        }
        else {
            processedPin += wordNumArr[Number(currentChar)] + ". ";
        }
    }
    return processedPin
}

function processUserId() {
    var userIdPrefix = "amzn1.ask.account.";
    var processedUserId = userId.substring(userIdPrefix.length);
    return encodeURIComponent(processedUserId);
}
