let json = require('../../testParameters.json');
let tests = Object.keys(json.tests);
let innerJson, method, endpoint, query, parameters, request, keys;

describe('Automated tests for the omar-oms API endpoints', () => {
    tests.forEach((test) => {
        innerJson = json.tests[test];
        method = innerJson["method"];
        endpoint = innerJson["endpoint"];
        query = innerJson["in"] === "query";
        parameters = Object.keys(innerJson["parameters"]);
        if(query) {
            request = "?"
            parameters.forEach((parameter) => {
                request = request + parameter + "=" + innerJson.parameters[parameter] + "&";
            })
            request = request.substring(0, request.length - 1);
            it(`Should test 200 code for ${test} default values`, () => {
                cy.request(method, endpoint + request)
                    .then((response) => {
                        expect(response.status).to.eq(200)
                    })
            })
            it(`Should test response header for ${test}`, () => {
                cy.request(method, endpoint + request)
                    .then((response) => {
                        expect(response).to.have.property("headers")
                    })
            })
        }
    })
})