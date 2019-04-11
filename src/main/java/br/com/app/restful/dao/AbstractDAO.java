package br.com.app.restful.dao;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.transaction.Transactional;

import br.com.app.restful.model.ModelEntity;

public abstract class AbstractDAO<T extends ModelEntity> implements DAO<T> {

	@PersistenceContext(type = PersistenceContextType.EXTENDED)
	EntityManager em;

	private final Class<T> clazz;
	protected CriteriaBuilder builder;
	protected CriteriaQuery<T> query;
	protected Root<T> root; 
	
	public AbstractDAO() {
		Type type = getClass().getGenericSuperclass();

		while (!(type instanceof ParameterizedType) || ((ParameterizedType) type).getRawType() != AbstractDAO.class) {
			if (type instanceof ParameterizedType) {
				type = ((Class<?>) ((ParameterizedType) type).getRawType()).getGenericSuperclass();
			} else {
				type = ((Class<?>) type).getGenericSuperclass();
			}
		}

		this.clazz = (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[0];
	}
	
	@PostConstruct
	private void init() {
		builder = em.getCriteriaBuilder();
	    query = builder.createQuery(clazz);
        root = (Root<T>) query.from(clazz).alias(clazz.getSimpleName().toLowerCase());
	}

	public List<T> list() {
		return em.createQuery("select o from " + clazz.getSimpleName() + " o").getResultList();
	}
	
	public List<T> listWithCriteria(final List<?> fields, final List<Predicate> restrictions) {
		
		Selection<?>[] selections = new Selection<?>[fields.size()];
		
		for (int i = 0; i < fields.size(); i++) {
			selections[i] = root.get(fields.get(i).toString());
		}
		
		query.select(builder.construct(clazz, selections));
		
		if ( !restrictions.isEmpty() ) {
			query.where(restrictions.toArray(new Predicate[restrictions.size()]));
		}
        
        TypedQuery<T> typedQuery = em.createQuery(query);
		
		return typedQuery.getResultList();
	}
	
	public List<T> listWithCriteria(final List<?> fields, final List<Predicate> restrictions, final List<String> joins) {
		
		
		joins.forEach( join -> root.join(join, JoinType.LEFT) );
		
		Selection<?>[] selections = new Selection<?>[fields.size()];
		
		for (int i = 0; i < fields.size(); i++) {
			selections[i] = root.get(fields.get(i).toString());
		}
		
		query.select(builder.construct(clazz, selections));
		
		if ( !restrictions.isEmpty() ) {
			query.where(restrictions.toArray(new Predicate[restrictions.size()]));
		}
        
        TypedQuery<T> typedQuery = em.createQuery(query);
		
		return typedQuery.getResultList();
	}

	public T find(Long id) {
		return em.find(clazz, id);
	}
	
	public T findWithCriteria(final Long id, final List<?> fields) {
		
		CriteriaBuilder builder = em.getCriteriaBuilder();
		builder = em.getCriteriaBuilder();
	    CriteriaQuery<T> query = builder.createQuery(clazz);
        Root<T> root = (Root<T>) query.from(clazz).alias(clazz.getSimpleName().toLowerCase());
        
        if ( !fields.isEmpty() ) {
			
			List<Expression<?>> fieldsList = new ArrayList<Expression<?>>();
			
			fields.forEach(f -> fieldsList.add(root.get(f.toString())));
			
			final List<Selection<?>> selectionList = new ArrayList<>();
			selectionList.addAll(fieldsList);
			
			query.multiselect(selectionList);
			
		}
        
        query.where(builder.and(builder.equal(root.get("id"), id)));
		
        TypedQuery<T> typedQuery = em.createQuery(query);
		
		return typedQuery.getSingleResult();
	}

	@Transactional
	public Long create(T entity) {
		em.persist(entity);
		em.flush();
		return entity.getId();
	}
	
	@Transactional
	public T update(T entity) {
		return em.merge(entity);
	}

	@Transactional
	public void remove(T entity) {
		em.remove(em.merge(entity));
	}

}
