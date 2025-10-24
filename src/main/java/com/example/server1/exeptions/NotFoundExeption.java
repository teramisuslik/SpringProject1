package com.example.server1.exeptions;

public class NotFoundExeption extends RuntimeException{

    public NotFoundExeption(String message){
        super(message);
    }
}
