package reactor.data.redis.test;

/**
 * @author Jon Brisbin
 */
public class Person {

	private String id;
	private String name;

	public Person() {
	}

	public Person(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public Person(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Person{" +
				"id='" + id + '\'' +
				", name='" + name + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if(this == o) {
			return true;
		}
		if(o == null || getClass() != o.getClass()) {
			return false;
		}

		Person person = (Person)o;

		if(!id.equals(person.id)) {
			return false;
		}
		if(!name.equals(person.name)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id.hashCode();
		result = 31 * result + name.hashCode();
		return result;
	}

}
