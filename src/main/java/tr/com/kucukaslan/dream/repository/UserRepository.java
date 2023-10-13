package tr.com.kucukaslan.dream.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import tr.com.kucukaslan.dream.model.User;

@Repository
public class UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    
}
