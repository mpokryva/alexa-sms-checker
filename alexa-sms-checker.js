var request = require('request');
var firebase = require('firebase');
var config = {
    apiKey: "AIzaSyDn9w41yXTYDH1eLqAedzfA_r-Q1F4EJzA",
    authDomain: "alexasmschecker.firebaseapp.com",
    databaseURL: "https://alexasmschecker.firebaseio.com",
    storageBucket: "alexasmschecker.appspot.com",
};
firebase.initializeApp(config);
// Get a reference to the database
var database = firebase.database();

exports.handler = (event, context) => {
    try {
        // New session
        if (event.session.new) {
            // New Session
            console.log("NEW SESSION");
        }

        // Launch Request
        switch (event.request.type) {
            case "LaunchRequest":
                var url = "https://api.random.org/json-rpc/1/invoke";
                var myRequest = {
                    "jsonrpc": "2.0",
                    "method": "generateStrings",
                    "params": {
                        "apiKey": "dccb74b2-80c6-42f0-81dd-b14205328f89",
                        "n": "1",
                        "length": "3",
                        "characters": "abcdefghijklmnopqrstuvwxyz0123456789"
                    },
                    "id": 96
                }
                var pin;
                request.post(
                    url,
                    {json: myRequest},
                    function (error, response, body) {
                        console.log("Hellloooo");
                        if (!error && response.statusCode == 200) {
                            pin = body.result.random.data[0];
                            var wordNumArr = ["zero", "one", "two", "three", "four",
                             "five", "six", "seven", "eight", "nine"];
                            var processedPin = "";
                            for (i = 0; i < pin.length; i++){
                                var currentChar = pin.charAt(i);
                                if (isNaN(Number(currentChar))){
                                    processedPin += currentChar + ". ";
                                }
                                else {
                                    processedPin += wordNumArr[Number(currentChar)] + ". ";
                                }
                            }
                            writeUserPin(pin);
                            var welcome = "Welcome to s. m. s checker. ";
                            var pinStatement = "Your 3 letter or number pin is: " + processedPin;
                            context.succeed(
                                generateResponse(
                                    buildSpeechletReponse(welcome + pinStatement, true),
                                    {}
                                )
                            );
                            console.log(pin);
                        }
                        else {
                            console.log(error);
                        }
                    }
                );
                console.log("LAUNCH REQUEST");
                break;
            // Intent Request
            case "IntentRequest":
                console.log("INTENT REQUEST");
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

function writeUserPin(pinValue) {
    console.log("writing stuff");
    firebase.database().ref('newPins').set({
        pin : pinValue
    });
}
