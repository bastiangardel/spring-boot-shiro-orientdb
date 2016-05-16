package com.github.pires.example.rest;

import com.github.pires.example.model.CheckOut;
import com.github.pires.example.model.User;
import com.github.pires.example.repository.CheckOutRepository;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Role;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Created by bastiangardel on 16.05.16.
 */
@RestController
@RequestMapping("/checkout")
public class CheckOutController {

    private static final Logger log = LoggerFactory.
            getLogger(UserController.class);


    @Autowired
    private CheckOutRepository checkoutRepo;

    @RequestMapping(method = POST)
    @RequiresAuthentication
    @RequiresRoles(value = {"SELLER", "ADMIN"}, logical = Logical.OR)
    public void create(@RequestBody CheckOut checkOut) {
        log.info("create new Checkout {}");

        checkoutRepo.save(checkOut);

    }
}