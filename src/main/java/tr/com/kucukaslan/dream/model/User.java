package tr.com.kucukaslan.dream.model;

import lombok.Data;

import java.io.Serializable;

import jakarta.persistence.Entity;

@Data
@Entity(name = "user")
public class User implements Serializable {

}
