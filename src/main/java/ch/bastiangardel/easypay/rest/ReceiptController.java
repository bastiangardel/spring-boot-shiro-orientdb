package ch.bastiangardel.easypay.rest;

import ch.bastiangardel.easypay.dto.*;
import ch.bastiangardel.easypay.exception.*;
import ch.bastiangardel.easypay.model.CheckOut;
import ch.bastiangardel.easypay.model.Receipt;
import ch.bastiangardel.easypay.model.User;
import ch.bastiangardel.easypay.repository.CheckOutRepository;
import ch.bastiangardel.easypay.repository.ReceiptRepository;
import ch.bastiangardel.easypay.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonView;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthorizedException;
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

import java.util.LinkedList;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Created by bastiangardel on 16.05.16.
 */
@RestController
@RequestMapping("/receipts")
public class ReceiptController {
    private static final Logger log = LoggerFactory.
            getLogger(ReceiptController.class);

    @Autowired
    private ReceiptRepository receiptRepo;

    @Autowired
    private CheckOutRepository checkOutRepo;

    @Autowired
    private UserRepository userRepo;

    @RequestMapping(method = POST)
    @RequiresAuthentication
    @RequiresRoles("SELLER")
    public SuccessMessageDTO create(@RequestBody ReceiptCreationDTO receiptCreationDTO){
        log.info("create new Receipt {}");

        CheckOut checkOut;


        checkOut = checkOutRepo.findByUuid(receiptCreationDTO.getUuidCheckout());

        if (checkOut == null)
            throw new CheckOutNotFoundException("Not Found CheckOut with UUID : " + receiptCreationDTO.getUuidCheckout());

        final Subject subject = SecurityUtils.getSubject();


        log.info("{} create new Receipt from {}", checkOut.getOwner().getEmail(), subject.getSession().getAttribute("email"));


        if(!checkOut.getOwner().getEmail().equals(subject.getSession().getAttribute("email")))
            throw new OwnerException("Your are not the owner of this checkout");

        Receipt receipt = receiptRepo.save(receiptCreationDTO.dtoToModel());

        checkOut.setLastReceipt(receipt);

        checkOutRepo.save(checkOut);

        return new SuccessMessageDTO("Creation with Success");
    }

    @JsonView(View.Summary.class)
    @RequestMapping(method = GET)
    @RequiresAuthentication
    @RequiresRoles("ADMIN" )
    public List<Receipt> getAll() {
        log.info("Get All Receipt");
        return (List<Receipt>) receiptRepo.findAll();
    }

    @RequestMapping(value = "/history", method = GET)
    @RequiresAuthentication
    public List<ReceiptHistoryDTO> getReceiptHistory(@RequestParam("uuid") String uuid){

        log.info("Get Receipt History from checkOut : {}", uuid);

        CheckOut checkOut;

        checkOut = checkOutRepo.findByUuid(uuid);
        if (checkOut == null)
            throw new CheckOutNotFoundException("Not Found CheckOut with UUID : " + uuid);

        List<ReceiptHistoryDTO> list = new LinkedList<>();
        for(Receipt receipt : checkOut.getReceiptsHistory())
        {
            ReceiptHistoryDTO receiptPayDTO = new ReceiptHistoryDTO();
            receiptPayDTO.modelToDto(receipt);
            list.add(receiptPayDTO);

        }


        return  list;

    }

    @RequestMapping(value = "/pay", method = GET)
    @RequiresAuthentication
    public ReceiptPayDTO getReceiptToPay(@RequestParam("uuid") String uuid){

        log.info("Get Receipt from checkOut : {}", uuid);

        CheckOut checkOut;

        checkOut = checkOutRepo.findByUuid(uuid);
        if (checkOut == null)
            throw new CheckOutNotFoundException("Not Found CheckOut with UUID : " + uuid);


        ReceiptPayDTO receiptPayDTO = new ReceiptPayDTO();

        Receipt receipt = checkOut.getLastReceipt();

        if (receipt == null)
            throw new NoReceiptToPayExeption("No Receipt to Pay");


        return  receiptPayDTO.modelToDto(receipt);

    }

    @RequestMapping(value = "/pay", method = POST)
    @RequiresAuthentication
    public SuccessMessageDTO paiement(@RequestBody ReceiptPayDTO receiptPayDTO, @RequestParam("uuid") String uuid){
        log.info("PayReceipt : {}", receiptPayDTO.getId());

        final Subject subject = SecurityUtils.getSubject();
        Receipt receipt;

        receipt = receiptRepo.findOne(receiptPayDTO.getId());

        if (receipt == null)
            throw new ReceiptNotFoundException("Not found Receipt with ID : " + receiptPayDTO.getId());


        CheckOut checkOut;

        checkOut = checkOutRepo.findByUuid(uuid);

        if (checkOut == null)
            throw new CheckOutNotFoundException("Not found CheckOut with UUID : " + uuid);

        User owner = checkOut.getOwner();

        if (receipt.isPaid())
            throw new NoReceiptToPayExeption("Receipt with id : " + receipt.getId() + " already pay");

        User user = userRepo.findByEmail((String) subject.getSession().getAttribute("email"));

        if (receipt.getAmount() > user.getAmount())
            throw new NotEnoughMoneyException("You have not enough money in your account!!");


        checkOut.setLastReceipt(null);
        List<Receipt> listreceipt = checkOut.getReceiptsHistory();
        listreceipt.add(receipt);
        receipt.setPaid(true);

        user.setAmount(user.getAmount() - receipt.getAmount());

        receipt.setPaiyedBy(user);

        owner.setAmount(owner.getAmount() + receipt.getAmount());

        List<Receipt> list = user.getReceiptHistory();
        list.add(receipt);

        userRepo.save(user);
        userRepo.save(owner);
        receiptRepo.save(receipt);
        checkOutRepo.save(checkOut);

        return new SuccessMessageDTO("Payment executed with Success");

    }

}
