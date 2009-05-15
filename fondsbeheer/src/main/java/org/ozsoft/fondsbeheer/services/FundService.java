package org.ozsoft.fondsbeheer.services;

import java.util.List;

import org.ozsoft.fondsbeheer.entities.Category;
import org.ozsoft.fondsbeheer.entities.Closing;
import org.ozsoft.fondsbeheer.entities.Fund;

/**
 * Interface of the Fund service.
 * 
 * @author Oscar Stigter
 */
public interface FundService {
    
    void connect() throws DatabaseException;
    
    void close() throws DatabaseException;
    
    List<Category> findCategories() throws DatabaseException;
    
    Category findCategory(String categoryId) throws DatabaseException;
    
    void storeCategory(Category category) throws DatabaseException;
    
    List<Fund> findFunds() throws DatabaseException;
    
    List<Fund> findFunds(String categoryId) throws DatabaseException;
    
    Fund findFund(String fundId) throws DatabaseException;
    
    void storeFund(Fund fund) throws DatabaseException;
    
    List<Closing> findClosings(String fundId) throws DatabaseException;
    
    void storeClosing(Closing closing) throws DatabaseException;

}