package ch.bastiangardel.easypay.rest;

import ch.bastiangardel.easypay.dto.CredentialDTO;
import ch.bastiangardel.easypay.dto.SuccessMessageDTO;
import ch.bastiangardel.easypay.exception.NotEnoughMoneyDebitException;
import ch.bastiangardel.easypay.exception.NotEnoughMoneyException;
import ch.bastiangardel.easypay.exception.UserNotFoundException;
import ch.bastiangardel.easypay.model.CheckOut;
import ch.bastiangardel.easypay.model.Permission;
import ch.bastiangardel.easypay.model.Role;
import ch.bastiangardel.easypay.model.User;
import ch.bastiangardel.easypay.repository.*;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ser.std.StdArraySerializers;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Created by bastiangardel on 16.05.16.
 *
 * Copyright (c) 2016 Bastian Gardel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.
            getLogger(UserController.class);

    @Autowired
    private DefaultPasswordService passwordService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private PermissionRepository permissionRepo;

    @Autowired
    private CheckOutRepository checkOutRepo;

    @Autowired
    private ReceiptRepository receiptRepo;

    @RequestMapping(value = "/auth", method = POST)
    public void authenticate(@RequestBody final CredentialDTO credentials) {

        final Subject subject = SecurityUtils.getSubject();

        log.info("Authenticating {}", credentials.getUsername() + " : " + subject.getSession().getHost());

        subject.login(credentials.daoToModel(subject.getSession().getHost()));
        // set attribute that will allow session querying
        subject.getSession().setAttribute("email", credentials.getUsername());

    }


    @RequestMapping(value = "/logout", method = POST)
    @RequiresAuthentication
    public void logout() {

        final Subject subject = SecurityUtils.getSubject();
        log.info("logout {}",subject.getSession().getAttribute("email"));
        subject.logout();
    }

    @RequestMapping(value = "/credit", method = POST)
    @RequiresAuthentication
    @RequiresRoles("ADMIN")
    public SuccessMessageDTO creditAccount(@RequestParam String username, @RequestParam Double credit){
        log.info("credit: {}" , username);

        User user = userRepo.findByEmail(username);

        if (user == null)
            throw new UserNotFoundException("Not found User with Username : " + username);

        Double amount = user.getAmount();
        user.setAmount(amount + credit);

        userRepo.save(user);

        return new SuccessMessageDTO("Credit with Success");
    }

    @RequestMapping(value = "/debit", method = POST)
    @RequiresAuthentication
    @RequiresRoles("ADMIN")
    public SuccessMessageDTO debitAccount(@RequestParam String username, @RequestParam Double debit){
        log.info("debit: {}" , username);

        User user = userRepo.findByEmail(username);

        if (user == null)
            throw new UserNotFoundException("Not found User with Username : " + username);

        if (debit > user.getAmount())
            throw new NotEnoughMoneyDebitException("There is not enough money in this account!!");

        Double amount = user.getAmount();
        user.setAmount(amount - debit);

        return new SuccessMessageDTO("Debit with Success");
    }

    @JsonView(View.Summary.class)
    @RequestMapping(method = GET)
    @RequiresAuthentication
    @RequiresRoles("ADMIN" )
    public List<User> getAll() {
        log.info("Get All User");
        return (List<User>)userRepo.findAll();
    }

    @RequestMapping(method = PUT)
    public void initScenario() {
        log.info("Initializing scenario..");
        // clean-up users, roles and permissions
        userRepo.deleteAll();
        roleRepo.deleteAll();
        permissionRepo.deleteAll();
        checkOutRepo.deleteAll();
        receiptRepo.deleteAll();
        // define permissions
        Permission p1 = new Permission();
        p1.setName("VIEW_ALL_USERS");
        p1 = permissionRepo.save(p1);
        Permission p2 = new Permission();
        p2.setName("DO_SOMETHING");
        p2 = permissionRepo.save(p2);
        Permission p3 = new Permission();
        p3.setName("SELLING");
        p3 = permissionRepo.save(p3);
        Permission p4 = new Permission();
        p4.setName("BUYING");
        p4 = permissionRepo.save(p4);
        // define roles
        Role roleAdmin = new Role();
        roleAdmin.setName("ADMIN");
        roleAdmin.getPermissions().add(p1);
        roleAdmin = roleRepo.save(roleAdmin);
        Role roleSeller = new Role();
        roleSeller.setName("SELLER");
        roleSeller.getPermissions().add(p3);
        roleSeller = roleRepo.save(roleSeller);
        Role roleClient = new Role();
        roleClient.setName("Client");
        roleClient.getPermissions().add(p4);
        roleClient = roleRepo.save(roleClient);
        // define user
        final User user = new User();
        user.setActive(true);
        user.setCreated(System.currentTimeMillis());
        user.setEmail("test@test.com");
        user.setName("Paulo Pires");
        user.setPassword(passwordService.encryptPassword("test"));
        user.getRoles().add(roleAdmin);
        user.setAmount(100.0);
        userRepo.save(user);

        final User user2 = new User();
        user2.setActive(true);
        user2.setCreated(System.currentTimeMillis());
        user2.setEmail("test2@test.com");
        user2.setName("Bastian Gardel");
        user2.setPassword(passwordService.encryptPassword("test"));
        user2.getRoles().add(roleSeller);
        user2.setAmount(100.0);

        User usercheckout = userRepo.save(user2);

        final User user3 = new User();
        user3.setActive(true);
        user3.setCreated(System.currentTimeMillis());
        user3.setEmail("test3@test.com");
        user3.setName("David Dupont");
        user3.setPassword(passwordService.encryptPassword("test"));
        user3.getRoles().add(roleSeller);
        user3.setAmount(100.0);
        userRepo.save(user3);

        CheckOut checkOut = new CheckOut();

        checkOut.setName("c1");
        checkOut.setOwner(usercheckout);
        checkOut.setUuid("C696E65A-CF37-659A-6D45-7DF4E396DA53");

        checkOut = checkOutRepo.save(checkOut);

        List<CheckOut> list = usercheckout.getCheckoutInPossesion();

        list.add(checkOut);

        userRepo.save(usercheckout);


        log.info("Scenario initiated.");
    }

}
