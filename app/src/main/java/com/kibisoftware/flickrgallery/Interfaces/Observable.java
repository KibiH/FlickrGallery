package com.kibisoftware.flickrgallery.Interfaces;

public interface Observable {
    void registerObserver(Observer observer);
    void notifyObservers();
}
