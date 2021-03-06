package com.vladkharchenko.coursework.spring.mycoursework.dao;

import com.vladkharchenko.coursework.spring.mycoursework.entity.Company;
import com.vladkharchenko.coursework.spring.mycoursework.entity.Genre;
import com.vladkharchenko.coursework.spring.mycoursework.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MoviesRepository extends JpaRepository<Movie,Integer> {
     List<Movie> getAllMoviesBy();
     List<Movie> findAllByGenres(Genre genre);

}

