describe('Automated tests for the omar-oms methods', () => {
            it(`Should test 200 code for getInfo default values`, () => {
                cy.request("get", "/dataInfo/getInfo?filename=%2Fdata%2Fs3%2Fadhoc%2F16SEP08110841-M1BS-055998376010_01_P006.TIF&entry=0")
                    .then((response) => {
                        expect(response.status).to.eq(200)
                    })
            })
            it(`Should test response header for getInfo`, () => {
                 cy.request("get", "/dataInfo/getInfo?filename=%2Fdata%2Fs3%2Fadhoc%2F16SEP08110841-M1BS-055998376010_01_P006.TIF&entry=0")
                     .then((response) => {
                         expect(response).to.have.property("headers")

                   })
               })
    })
