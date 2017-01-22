var request = require('request');
var firebase = require('firebase');
var config = {
    apiKey: "my-api-key",
    authDomain: "my-auth-domain",
    databaseURL: "my-url",
    storageBucket: "my-storage-bucket"
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
            case "LaunchRequest":
                // Check if user already exists
                var exists;
                rootRef.ref("/users/" + processUserId()).once("value").then(function(snapshot) {
                    exists = (snapshot.val() != null);
                    if (!exists) {
                        generatePin(context);
                    } else {
                        context.succeed(
                            generateResponse(
                                buildSpeechletReponse("You're all set up!", true),
                                {}
                            )
                        );
                    }
                });
                console.log("LAUNCH REQUEST");
                break;
            // Intent Request
            case "IntentRequest":
                console.log("INTENT REQUEST");

                switch (event.request.intent.name) {
                    case "GetUnreadMessageCount":
                        console.log("GetUnreadMessageCount Intent");
                        var intentRef = makeIntentRef();
                        setGetUnreadCountRef(intentRef);
                        setTimeout(function() {
                            retrieveAndSpeakCount(intentRef, context);
                        }, 2000);
                        break;
                    case "GeneratePin":
                        console.log("GeneratePin Intent");
                        generatePin(context);
                        break;
                    case "MakeCall":
                        console.log("MakeCall Intent");
                        var contactName = event.request.intent.slots.ContactName.value;
                        var phoneNumber = event.request.intent.slots.PhoneNumber.value;
                        var recipient;
                        var targetRef = makeIntentRef();
                        // Checking if user said a number or a name.
                        if (contactName != undefined) {
                            recipient = contactName;
                        } else {
                            recipient = phoneNumber;
                        }
                        setMakeCallRef(recipient, targetRef);
                        // If user said a number, process it for speech.
                        if (recipient == phoneNumber) {
                            //recipient = processPinForSpeech(phoneNumber);
                            recipient = processPhoneNumberForSpeech(phoneNumber);
                        }
                        setTimeout(function() {
                            checkCallSucess(recipient, targetRef).then(function(outputText){
                                speakAndCleanup(outputText, targetRef, context);
                            });
                        }, 5000);
                        break;
                    case "SendSMS":
                        console.log("SendSMS Intent");
                        var recipient = event.request.intent.slots.Recipient.value;
                        var messageBody = event.request.intent.slots.MessageBody.value;
                        setSendSMSRef(recipient, messageBody);
                        context.succeed(
                            generateResponse(
                                buildSpeechletReponse("Sending a message to " + recipient, true),
                                {}
                            )
                        );
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

function retrieveAndSpeakCount(targetRef, context) {
    console.log("testing retrieveAndSpeakCount");
    var unreadCount;
    targetRef.once("value")
    .then(function (snapshot) {
        sayUnreadCount(snapshot, context, targetRef);
    });
}


function processPhoneNumberForSpeech(phoneNumber) {
    var processedPhoneNumber = "";
    for (i = 0; i < phoneNumber.length - 1; i++) {
        processedPhoneNumber += phoneNumber.charAt(i) + ", ";
    }
    processedPhoneNumber += phoneNumber.charAt(phoneNumber.length - 1) + ".";
    return processedPhoneNumber;
}



function makeIntentRef(){
    return rootRef.ref("users/" + processUserId() + "/intentQueue/" + new Date().getTime());
}

function checkCallSucess(recipient, targetRef) {
    console.log("Checking success");
    var outputText;
    return targetRef.once("value")
    .then(function (snapshot) {
        var numberFound = snapshot.val().numberFound;
        if (numberFound != undefined) {
            if (numberFound) {
                console.log("1.1");
                outputText = "Making a call to " + recipient;
            } else {
                console.log("1.2");
                outputText = "Contact was not found. Call could not be made"
            }
        } else {
            console.log("2.0");
            outputText = "Call to " + recipient + " may have not been successful";
        }
        var outputTextPromise = new Promise(function(resolve, reject) {
            resolve(outputText);
        });
        return outputTextPromise;
    });
}




function setGetUnreadCountRef(targetRef) {
    targetRef.set({
        intentName: "GetUnreadMessageCount"
    });
}

function setMakeCallRef(recipient, targetRef) {
    targetRef.set({
        intentName: "MakeCall",
        recipient: recipient
    });
}

function setSendSMSRef(recipient, messageBody) {
    var targetRef = makeIntentRef();
    targetRef.set({
        intentName: "SendSMS",
        recipient: recipient,
        messageBody: messageBody
    });
}

function processPinForSpeech(pin) {
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

function processNumberForSpeech(digit) {
    var wordNumArr = ["zero", "one", "two", "three", "four",
     "five", "six", "seven", "eight", "nine"];
     if (isNaN(Number(digit))) {
         return null;
     } else {
         return wordNumArr[Number(digit)];
     }
}

function processUserId() {
    var userIdPrefix = "amzn1.ask.account.";
    var processedUserId = userId.substring(userIdPrefix.length);
    return encodeURIComponent(processedUserId);
}


function sayUnreadCount(snapshot, context, targetRef) {
    var unreadCount = snapshot.val().result;
    var outputText = "";
    console.log(unreadCount);
    if (unreadCount != null) {
        console.log("Got unread count");
        var unreadMessagesLiteral = "";
        if (unreadCount == 1){
            unreadMessagesLiteral = " unread message.";
        } else {
            unreadMessagesLiteral = " unread messages.";
        }
        outputText = "You have " + processNumberForSpeech(unreadCount) + unreadMessagesLiteral;
    }
    else {
        console.log("Didn't get unread count");
        outputText = "Couldn't get unread message count";
    }
    speakAndCleanup(outputText, targetRef, context);
}

function speakAndCleanup(outputText, targetRef, context) {
    console.log("cleaning up")
    targetRef.remove()
    .then(function() {
        context.succeed(
            generateResponse(
                buildSpeechletReponse(outputText, true),
                {}
            )
        )
    });
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


function generatePin(context) {
    var url = "https://api.random.org/json-rpc/1/invoke";
    var myRequest = {
        "jsonrpc": "2.0",
        "method": "generateStrings",
        "params": {
            "apiKey": "my-api-key",
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
            if (!error && response.statusCode == 200) {
                //console.log(this);
                pin = body.result.random.data[0];
                var welcome = "Welcome to s.m.s checker. ";

                rootRef.ref("newPins/" + pin).set({
                    phoneNumber : "",
                    userId: processUserId()
                });

                var pinStatement = "Your 3 letter or number pin is: " + processPinForSpeech(pin);
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
}
