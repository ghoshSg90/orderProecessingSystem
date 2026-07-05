package com.test.orderProcessingSystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "DUAL")
public class Dual {

    @Id
    @Column(name = "DUMMY", nullable = true)
    private String dummy;

    public Dual() {
        super();
    }

    public String getDummy() {
        return dummy;
    }

    public void setDummy(String dummy) {
        this.dummy = dummy;
    }

}
