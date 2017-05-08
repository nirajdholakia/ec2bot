/*-----------------------------------------------------------------------------
This is Amazon EC2 bot. It can provide answers to user queries about amazon EC2
service and also provide best instance information for a given user configuration

# RUN THE BOT:

    Run the bot from the command line using "node app.js" and then type 
    "hello" to wake the bot up.
    The bot is to be deployed on azure so as to get it on skype
-----------------------------------------------------------------------------*/
var restify = require('restify');
var builder = require('botbuilder');
var cognitiveservices = require('botbuilder-cognitiveservices');
var intersect = require('intersect');
var http = require('http');

//=========================================================
// Restify Server Setup
//=========================================================
var server = restify.createServer();
server.listen(process.env.port || process.env.PORT || 3978, function() {
    console.log('%s listening to %s', server.name, server.url);
});

//=========================================================
// Chat Connector
//=========================================================  

// Create chat connector for communicating with the Bot Framework Service
var connector = new builder.ChatConnector({
    appId: process.env.MICROSOFT_APP_ID,
    appPassword: process.env.MICROSOFT_APP_PASSWORD
});

var bot = new builder.UniversalBot(connector);
// Listen for messages from users 
server.post('/api/messages', connector.listen());

//=========================================================
// LUIS Definations
//=========================================================

// NLP code - Make sure you add code to validate these fields
var luisAppId = process.env.LuisAppId;
var luisAPIKey = process.env.LuisAPIKey;
var luisAPIHostName = process.env.LuisAPIHostName || 'api.projectoxford.ai';
const LuisModelUrl = 'https://westus.api.cognitive.microsoft.com/luis/v2.0/apps/44a72edb-ad68-4214-9af2-2b8e2943b89a?subscription-key=274a03130a9141a3b10eba6a0b617cd6&verbose=true&timezoneOffset=-5.0&spellCheck=true&q=';

// Main dialog with LUIS
var recognizer = new builder.LuisRecognizer(LuisModelUrl);
var dialog = new builder.IntentDialog({
    recognizers: [recognizer]
});

//=========================================================
// QnA Maker Dialogs
//=========================================================
// QnA Maker Dialogs
var recognizer = new cognitiveservices.QnAMakerRecognizer({
    knowledgeBaseId: 'cfec11a1-c246-478b-931b-719e96eb38c2',
    subscriptionKey: '48151ec1e8fb4dfa9abf84047bb8c2fc'
});

var BasicQnAMakerDialog = new cognitiveservices.QnAMakerDialog({
    recognizers: [recognizer],
    defaultMessage: 'Hey! I will answer any questions about Amazon EC2',
    qnaThreshold: 0.5
});
//=========================================================
// Bots Variables
//=========================================================
var inHours = false;
var inDays = false;
var inYear = false;
var fromNoMatch = false;
var a = [];
var b = [];
var instance = [];

//=========================================================
// Bots Dialogs
//=========================================================

//the very first dialog for the bot!
bot.dialog('/', [
    function(session) {
        session.send("Hey! EC2 Bot here...");
        session.beginDialog('help');
    }
]);

//Dialog when user types Help or Back
bot.dialog('help', [
    function(session) {
        session.endDialog("Just say \n\n* Info - to know more about EC2 services.\n* Estimate - if you already know your requirements.\n* Quit - End this conversation.\n* help/back - Displays this Menu.");
    }
]).triggerAction({
    matches: /help|back/i
});

//Dialog for using logic from QnA Maker
bot.dialog('info', BasicQnAMakerDialog).triggerAction({
    matches: /info|information/i
});

// Dialog for Estimator once Budget requirements are taken from the user
bot.dialog('estimator', [
    function(session, args) {
        if (!session.userData.budget && !session.userData.pricingmodel && !session.userData.duration && !session.userData.os && !session.userData.memory) {
            session.beginDialog('queryA1'); //start with Budget 	    
        } else if (session.userData.budget && !session.userData.pricingmodel && !session.userData.duration && !session.userData.os && !session.userData.memory) {
            session.beginDialog('queryA2'); //start with pricing model    
        } else if (session.userData.budget && session.userData.pricingmodel && !session.userData.duration && !session.userData.os && !session.userData.memory) {
            session.beginDialog('queryA'); //start with duration    
        } else if (session.userData.budget && session.userData.pricingmodel && session.userData.duration && !session.userData.os && !session.userData.memory) {
            session.beginDialog('queryB'); //start with os
        } else if (session.userData.budget && session.userData.pricingmodel && session.userData.duration && session.userData.os && !session.userData.memory) {
            session.beginDialog('queryC'); //start with memory
        } else if (session.userData.budget && session.userData.pricingmodel && session.userData.duration && session.userData.os && session.userData.memory) {
            session.beginDialog('summary'); //start summary
        }
    }
]).triggerAction({
    matches: /estimate/i
});

//Dialog jump from estimatorContinued when Budget, Pricing model, Duration, OS and Memory is unknown and rest of entities are know 
bot.dialog('queryA1', [
    function(session, args) {
        builder.Prompts.number(session, "What's your Budget in US Dollars");
    },
    function(session, results) {
        session.userData.budget = results.response;
        builder.Prompts.choice(session, "What Pricing model do you wish to choose?", ["ondemand", "reserved"]);
    },
    function(session, results) {
        session.userData.pricingmodel = results.response.entity;
        builder.Prompts.choice(session, "Duration for instance is in...?", ["Hours", "Days", "Years"]);
    },
    function(session, results) {
        if (results.response.entity.toLowerCase() == "hours") {
            inHours = true;
            builder.Prompts.number(session, "How many Hours?");
        } else if (results.response.entity.toLowerCase() == "days") {
            inDays = true;
            builder.Prompts.number(session, "How many Days?");
        } else if (results.response.entity.toLowerCase() == "years") {
            inYear = true;
            builder.Prompts.number(session, "How many Years?");
        }
    },
    function(session, results) {
        if (results.response == 0) {
            session.send("The duration cannot be zero. Lets try again");
            inHours = false;
            inDays = false;
            inYear = false;
            session.beginDialog('estimator');
        } else {
            if (inHours) {
                session.userData.duration = results.response;
            } else if (inDays) {
                session.userData.duration = results.response * 24;
            } else if (inYear) {
                session.userData.duration = results.response * 8760;
            }
            builder.Prompts.choice(session, "Operating System of the instances is...", ["windows", "linux"]);
        }
    },
    function(session, results) {
        session.userData.os = results.response.entity;
        builder.Prompts.number(session, "How much memory do you need in GB?");
    },
    function(session, results) {
        session.userData.memory = results.response;
        session.beginDialog('summary');
    }
]);

//Dialog jump from estimatorContinued when Pricing model, Duration, OS and Memory is unknown and rest of entities are know 
bot.dialog('queryA2', [
    function(session, args) {
        builder.Prompts.choice(session, "What Pricing model do you wish to choose?", ["ondemand", "reserved"]);
    },
    function(session, results) {
        session.userData.pricingmodel = results.response.entity;
        builder.Prompts.choice(session, "Duration for instance is in...?", ["Hours", "Days", "Years"]);
    },
    function(session, results) {
        if (results.response.entity.toLowerCase() == "hours") {
            inHours = true;
            builder.Prompts.number(session, "How many Hours?");
        } else if (results.response.entity.toLowerCase() == "days") {
            inDays = true;
            builder.Prompts.number(session, "How many Days?");
        } else if (results.response.entity.toLowerCase() == "years") {
            inYear = true;
            builder.Prompts.number(session, "How many Years?");
        }
    },
    function(session, results) {
        if (results.response == 0) {
            session.send("The duration cannot be zero. Lets try again");
            inHours = false;
            inDays = false;
            inYear = false;
            session.beginDialog('estimator');
        } else {
            if (inHours) {
                session.userData.duration = results.response;
            } else if (inDays) {
                session.userData.duration = results.response * 24;
            } else if (inYear) {
                session.userData.duration = results.response * 8760;
            }
            builder.Prompts.choice(session, "Operating System of the instances is...", ["windows", "linux"]);
        }
    },
    function(session, results) {
        session.userData.os = results.response.entity;
        builder.Prompts.number(session, "How much memory do you need in GB?");
    },
    function(session, results) {
        session.userData.memory = results.response;
        session.beginDialog('summary');
    }
]);

//Dialog jump from estimatorContinued when Duration, OS and Memory is unknown and rest of entities are know 
bot.dialog('queryA', [
    function(session, args) {
        builder.Prompts.choice(session, "Duration for instance is in...?", ["Hours", "Days", "Years"]);
    },
    function(session, results) {
        if (results.response.entity.toLowerCase() == "hours") {
            inHours = true;
            builder.Prompts.number(session, "How many Hours?");
        } else if (results.response.entity.toLowerCase() == "days") {
            inDays = true;
            builder.Prompts.number(session, "How many Days?");
        } else if (results.response.entity.toLowerCase() == "years") {
            inYear = true;
            builder.Prompts.number(session, "How many Years?");
        }
    },
    function(session, results) {
        if (results.response == 0) {
            session.send("The duration cannot be zero. Lets try again");
            inHours = false;
            inDays = false;
            inYear = false;
            session.beginDialog('estimator');
        } else {
            if (inHours) {
                session.userData.duration = results.response;
            } else if (inDays) {
                session.userData.duration = results.response * 24;
            } else if (inYear) {
                session.userData.duration = results.response * 8760;
            }
            builder.Prompts.choice(session, "Operating System of the instances is...", ["windows", "linux"]);
        }
    },
    function(session, results) {
        session.userData.os = results.response.entity;
        builder.Prompts.number(session, "How much memory do you need in GB?");
    },
    function(session, results) {
        session.userData.memory = results.response;
        session.beginDialog('summary');
    }
]);

//Dialog jump from estimatorContinued when OS and Memory are unknown and rest of entities are know
bot.dialog('queryB', [
    function(session, args) {
        builder.Prompts.choice(session, "Operating System of the instances is...", ["windows", "linux"]);
    },
    function(session, results) {
        session.userData.os = results.response.entity;
        builder.Prompts.number(session, "How much memory do you need in GB?");
    },
    function(session, results) {
        session.userData.memory = results.response;
        session.beginDialog('summary');
    }
]);

//Dialog jump from estimatorContinued when Memory is unknown and rest of entities are know
bot.dialog('queryC', [
    function(session, args) {
        builder.Prompts.number(session, "How much memory do you need in GB?");
    },
    function(session, results) {
        session.userData.memory = results.response;
        session.beginDialog('summary');
    }
]);

//Dialog when all the information for Estimation is obtained by the bot
//Calculations based of data obtained from user will be done here
bot.dialog('summary', [
    function(session, args) {
        //http getting instance and price based on budget, os, duration & pricing model
        session.send("Got it... Your Budget is $" + session.userData.budget +
            " using a '" + session.userData.pricingmodel + "' " + session.userData.os + " instance with " + session.userData.memory + "  GB memory" + " for time  " + session.userData.duration + " hours");
        var options = {
            host: 'ec2-54-245-141-112.us-west-2.compute.amazonaws.com',
            port: 8080,
            path: '/filterbycost/' + session.userData.pricingmodel + '/' + session.userData.os + '/' + (session.userData.budget / session.userData.duration)
        };
        http.get(options, function(res) {
            res.on("data", function(chunk) {
                a = JSON.parse(chunk);
                console.log("\n\n---------The value of A is-------\n\n" + a);
                for (var key in a) {
                    instance.push(key);
                }
                console.log("\n\n---------The value of Inside http.get instance is-------\n\n" + instance);
            });
        }).on('error', function(e) {
            console.log("Got error in options field: \n\n" + e.message);
        });
        console.log("\n\n---------The value of Out instance is-------\n\n" + instance);

        //http getting list of instances with greater than or equal to a given memory value
        var options2 = {
            host: 'ec2-54-245-141-112.us-west-2.compute.amazonaws.com',
            port: 8080,
            path: '/filterbymemory/' + session.userData.memory
        };

        http.get(options2, function(res2) {
            res2.on("data", function(chunk2) {
                b = JSON.parse(chunk2);
                console.log("\n\n---------The value of B is-------\n\n" + b);
            });
        }).on('error', function(e) {
            console.log("Got error in options2: \n\n" + e.message);
        });
        builder.Prompts.choice(session, "Is the information correct?", ["Yes", "No"]);
    },
    function(session, results) {
        if (results.response.entity.toLowerCase() == "yes") {
            session.beginDialog('calculator');
        } else {
            session.userData = {};
            inHours = false;
            inDays = false;
            inYear = false;
			fromNoMatch = false;
            a = [];
            b = [];
            instance = [];
            session.beginDialog('estimator');
        }
    }
]);

bot.dialog('calculator', [
    function(session, args) {
        setTimeout(session.beginDialog('calculator1'), 2000);
    }
]);


//Calculations for User Requirements
bot.dialog('calculator1', [
    function(session) {
        var result = intersect(b, instance);
        console.log(result);
        var body = '';
        for (var k in result) {
            body += " " + result[k] + " with price $" + (a[result[k]] * session.userData.duration).toFixed(2) + ",\n\n";
        }
        if (!body) {
            session.send("Sorry! There are no EC2 instances matching your requirements. Try something with a different configuration");
            fromNoMatch = true;
            session.beginDialog('wipeUserData');
        } else {
            session.send("Suitable instances matching your requirements are- \n\n" + body);
            builder.Prompts.choice(session, "You can type info to know more about EC2. Are you done with the Estimation?", ["Yes", "No"]);
        }
    },
    function(session, results) {
        if (!results.response.entity) {
            session.endDialog("Bye!");
        } else {
            if (results.response.entity.toLowerCase() == "yes") {
                session.beginDialog('wipeUserData');
            } else {
                session.userData = {};
                inHours = false;
                inDays = false;
                inYear = false;
				fromNoMatch = false;
                a = [];
                b = [];
                instance = [];
                session.beginDialog('estimator');
            }
        }
    }
]);

//Dialog when user says Quit or when Estimation task is completed
bot.dialog('wipeUserData', [
    function(session, args) {
        if (fromNoMatch == true) {
            session.userData = {};
            inHours = false;
            inDays = false;
            inYear = false;
            fromNoMatch = false;
            a = [];
            b = [];
            instance = [];
            session.beginDialog('estimator');
        } else {
            session.userData = {};
            inHours = false;
            inDays = false;
            inYear = false;
			fromNoMatch = false;
            a = [];
            b = [];
            instance = [];
            session.endDialog("Thank you for using Amazon EC2 Bot, see you until nexttime...");
        }
    }
]).triggerAction({
    matches: /quit|exit|goodbye|bye/i
});