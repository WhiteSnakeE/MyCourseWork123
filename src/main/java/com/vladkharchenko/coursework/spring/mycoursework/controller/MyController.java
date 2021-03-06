package com.vladkharchenko.coursework.spring.mycoursework.controller;

import com.vladkharchenko.coursework.spring.mycoursework.dao.*;
import com.vladkharchenko.coursework.spring.mycoursework.entity.*;
import com.vladkharchenko.coursework.spring.mycoursework.service.CustomerService;
import com.vladkharchenko.coursework.spring.mycoursework.service.MoviesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class MyController {
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private MoviesRepository moviesRepository;
    @Autowired
    private IssuingfilmRep issuingfilmRep;
    @Autowired
    private MoviesService moviesService;
    @Autowired
    private CompanyRep companyRep;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private GenreRep genreRep;

    @GetMapping({"/", ""})
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/registration")
    public String registration(Model customerModel, Model customerInfoModel) {
        Customer customer = new Customer();
        Customerinfo customerinfo = new Customerinfo();
        customerModel.addAttribute("customer", customer);
        customerInfoModel.addAttribute("customerInfo", customerinfo);

        return "registration";
    }

    @PostMapping("/registration")
    public String addUser(Customer customer, Customerinfo customerinfo,Model model) {
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        customer.setRole(Role.USER);
        customer.setCustomerinfo(customerinfo);
        customerinfo.setCustomer(customer);
        try{
            customerRepository.save(customer);
        }catch(JpaSystemException ex){
            model.addAttribute("error",ex.getCause().getCause().getMessage());
            model.addAttribute("customerInfo",customerinfo);
            return "registration";
        }

        return "redirect:/";
    }

    @GetMapping("/information")
    public String information(Principal principal, Model model, Model customerModel) {
        Customer customer = customerRepository.findByLogin(principal.getName());
        Customerinfo customerinfo = customer.getCustomerinfo();
        model.addAttribute("info", customerinfo);
        customerModel.addAttribute("infoCus", customer);
        model.addAttribute("status", customer.getUserSubscribe().getStatus());
        model.addAttribute("EndOfSub", customer.getUserSubscribe().getEnd());
        return "information";

    }
    @GetMapping("/information/new")
    public String editInfo(Principal principal, Model customerInfoModel, Model customerModel) {
        Customer customer = customerRepository.findByLogin(principal.getName());
        Customerinfo customerinfo = customer.getCustomerinfo();
        customerInfoModel.addAttribute("customerInfo1", customerinfo);
        customerModel.addAttribute("customer1", customer);
        return "edit-info";
    }
    @GetMapping("/subscribe")
    public String customerSubscribe(Principal principal,Model model){
        Customer customer = customerRepository.findByLogin(principal.getName());
        Status status = customer.getUserSubscribe().getStatus();
        UserSubscribe userSubscribe = customer.getUserSubscribe();
        model.addAttribute("SubStatus",status);
        model.addAttribute("sub",userSubscribe );
        return "subscribe";
    }

    @GetMapping("/subscribe/done")
    public String buySub(Principal principal){
        Customer customer = customerRepository.findByLogin(principal.getName());
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(1);
        customer.getUserSubscribe().setStatus(Status.PAID);
        customer.getUserSubscribe().setStart(startDate);
        customer.getUserSubscribe().setEnd(endDate);
        customerRepository.save(customer);
        return "index";
    }
    @PostMapping("/information/new")
    public String setInfo(Customerinfo customerinfo, Principal principal,
                          @ModelAttribute Customer customer1, BindingResult result,Model model) {
        Customer customer = customerRepository.findByLogin(principal.getName());
        if (!customer1.getPassword().equals("")) {
            if (!passwordEncoder.matches(customer1.getPassword(), customer.getPassword())) {
                customer.setPassword(passwordEncoder.encode(customer1.getPassword()));
            } else {
                ObjectError error = new ObjectError("globalError",
                        "The password must not be the same as the old one.");
                result.addError(error);
                System.out.println(result.hasErrors());
                return "redirect:/information";
            }
        }
        customer.getCustomerinfo().setName(customerinfo.getName());
        customer.getCustomerinfo().setSurname(customerinfo.getSurname());
        customer.getCustomerinfo().setAge(customerinfo.getAge());
        customer.getCustomerinfo().setMobilePhone(customerinfo.getMobilePhone());
        customerinfo.setCustomer(customer);
//        customer.setLogin(customer1.getLogin());
        customer.setEmail(customer1.getEmail());
        try{
            customerRepository.save(customer);
        }
        catch(JpaSystemException ex) {
            model.addAttribute("error", ex.getCause().getCause().getMessage());
            model.addAttribute("infoCus", customer);
            model.addAttribute("info", customerinfo);
            return "information";
        }
        return "redirect:/information";
    }
    @GetMapping("/movies")
    public String showAllMovies(Model model,
                                @RequestParam(name ="genre",required = false)String name,
                                @RequestParam(name ="selectCompany",required = false)String companyName) {
        List<Movie> movies = moviesRepository.getAllMoviesBy();
        List<Company> companies = companyRep.findAll();
        List<Movie> genresMoviesList ;
        if (name != null && name.length()>0) {
            genresMoviesList = moviesRepository.findAllByGenres(genreRep.findByName(name));
        } else {
            genresMoviesList = moviesRepository.findAll();
        }
        List<Genre> genres = genreRep.findAll();
        model.addAttribute("Movies",genresMoviesList);
        model.addAttribute("genres",genres);
        model.addAttribute("companies",companies);
        return "movies";
    }
    @GetMapping("/movies/{id}")
    public String showMovie(@PathVariable int id, Model model, Principal principal) {
        Movie movie = moviesService.getMovie(id);
        if (principal != null) {
            if (movie.getAvailable() != Available.AVAILABLE) {
                return "errorPage";
            }
            Customer customer = customerRepository.findByLogin(principal.getName());
            List<Issuingfilm> issuingfilmList = customer.getIssuingfilms();
            Status status = customer.getUserSubscribe().getStatus();
            int result;
            int count = 0;
            LocalDate startDate = LocalDate.now();
            for (Issuingfilm issuing : issuingfilmList) {
                result = startDate.compareTo(issuing.getReturnData());
                if (issuing.getMovie().getId_movies() == id && result < 0) {
                    count += 1;
                }
            }
            model.addAttribute("IsAlreadyBought", count);
            model.addAttribute("status",status);
        }
        model.addAttribute("Movie", movie);
//        model.addAttribute("MovieCompany", movie.getCompany().getCompanyName());
        model.addAttribute("MovieActors", movie.getActors());
        model.addAttribute("MovieAuthor", movie.getAuthor());
        model.addAttribute("MovieGenres", movie.getGenres());

        return "movies-watch";
    }

    @GetMapping("/library")
    public String library(Principal principal, Model model,Issuingfilm issuingfilm) {
        LocalDate startDate = LocalDate.now();
        Customer customer = customerRepository.findByLogin(principal.getName());
        if(customer == null){
            return "login";
        }
        List<Issuingfilm> issuingfilmList = customer.getIssuingfilms();
        model.addAttribute("YourFilms", customer.getIssuingfilms());
        return "customer-movie-library";
    }
    @PostMapping("/movies/{id}")
    public String buyMovie(@PathVariable int id, Principal principal) {
        Customer customer = customerRepository.findByLogin(principal.getName());
        Movie movie = moviesService.getMovie(id);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(1);
        Issuingfilm issuingfilm = new Issuingfilm(startDate,endDate,movie,customer);
        issuingfilmRep.save(issuingfilm);
        return "index";
    }

}
