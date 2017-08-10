'use strict';

describe('angularjs homepage', function() {
  it('should greet the named user', function() {
    browser.get('/');

    var userNameElement = element(by.model('loginController.userName'));
    var passwordElement = element(by.model('loginController.password'));
    var submitElement = element(by.id('login'));

    userNameElement.sendKeys('root');
    passwordElement.sendKeys('root');
    submitElement.click();

    expect(browser.getCurrentUrl()).toEqual('http://localhost:10080/#/my-data/LILS');

  });
});
