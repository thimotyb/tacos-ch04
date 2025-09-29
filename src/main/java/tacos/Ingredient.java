package tacos;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@NoArgsConstructor(access=AccessLevel.PRIVATE, force=true)
@Entity
public class Ingredient {
  
  @Id
  private  String id;
  private  String name;
  private  Type type;
  
  
  
  public Ingredient() {
	super();
 }


public Ingredient(String id, String name, Type type) {
	super();
	this.id = id;
	this.name = name;
	this.type = type;
}



public String getName() {
	return name;
}


public void setName(String name) {
	this.name = name;
}


public Type getType() {
	return type;
}


public void setType(Type type) {
	this.type = type;
}



public static enum Type {
    WRAP, PROTEIN, VEGGIES, CHEESE, SAUCE
  }

}
