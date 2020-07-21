let json = require('../../plugins/omar-oms-plugin/build/swaggerSpec.json');
let paths = Object.keys(json.paths);
let methods, innerJson, name, parameters, request;

describe('Automated tests for the omar-oms methods', () => {
    paths.forEach((path) => {

            methods = Object.keys(json.paths[path]);
            methods.forEach((method) => {
            innerJson = json.paths[path][method];
            name = innerJson["operationId"];
            parameters = innerJson["parameters"]
            request = "?"
            parameters.forEach((parameter) => {

                if(parameter["default"])
                    request = request + parameter["name"] + "=" + parameter["default"] + "&";
                else if(parameter["enum"])
                    request = request + parameter["name"] + "=" + parameter["enum"][0] + "&";
            })

            request = request.substring(0, request.length - 1);
            it(`Should test 200 code for ${name} default values`, () => {
                cy.request(method, path + request)
                    .then((response) => {
                        expect(response.status).to.eq(200)
                    })
            })
            it(`Should test response header for ${name}`, () => {
                 cy.request(method, path + request)
                     .then((response) => {
                         expect(response).to.have.property("headers")

                   })
               })
            })
        })
    })
