package com.example.todolist.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import com.example.todolist.common.Utils;
import com.example.todolist.entity.Todo;
import com.example.todolist.entity.Todo_;
import com.example.todolist.form.TodoQuery;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TodoDaoImpl implements TodoDao {
	private final EntityManager entityManager;
	
	@Override
	public List<Todo> findByJPQL(TodoQuery todoQuery) {
		StringBuilder sb = new StringBuilder("select t from Todo t where 1 = 1");
		List<Object> params = new ArrayList<>();
		int pos = 0;
		
		if(todoQuery.getTitle().length() > 0) {
			sb.append("and t.title like?" + (++pos));
			params.add("%" + todoQuery.getTitle() + "%");
		}
		
		if(todoQuery.getImportance() != -1) {
			sb.append("and t.importance = ?" + (++pos));
			params.add(todoQuery.getImportance());
		}
		
		if(todoQuery.getUrgency() != -1) {
			sb.append("and t.urgency = ?" + (++pos));
			params.add(todoQuery.getUrgency());
		}
		
		if(!todoQuery.getDeadlineFrom().equals("")) {
			sb.append("andt.deadline >= ?" + (++pos));
			params.add(Utils.str2date(todoQuery.getDeadlineFrom()));
		}
		
		if(!todoQuery.getDeadlineTo().equals("")) {
			sb.append("add t.deadline <= ?" + (++pos));
			params.add(Utils.str2date(todoQuery.getDeadlineTo()));
		}
		
		if(todoQuery.getDone() != null && todoQuery.getDone().equals("Y")) {
			sb.append("and t.done = ?" + (++pos));
			params.add(todoQuery.getDone());
		}
		
		sb.append("order by id");
		
		Query query = entityManager.createQuery(sb.toString());
		for(int i= 0; i < params.size(); ++i) {
			query = query.setParameter(i+1, params.get(i));
		}
		
		@SuppressWarnings("unchecked")
		List<Todo> list = query.getResultList();
		return list;
	}
	
	@Override
	public Page<Todo> findByCriteria(TodoQuery todoQuery, Pageable pageable) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Todo> query = builder.createQuery(Todo.class);
		Root<Todo> root = query.from(Todo.class);
		List<Predicate> predicates = new ArrayList<>();
		
		String title = "";
		if(todoQuery.getTitle().length() > 0) {
			title = "%" + todoQuery.getTitle() + "%";
		} else {
			title = "%";
		}
		predicates.add(builder.like(root.get(Todo_.TITLE), title));
		
		if(todoQuery.getImportance() != -1) {
			predicates.add(
					builder.and(
							builder.equal(root.get(Todo_.IMPORTANCE), todoQuery.getImportance())));
		}
		
		if(todoQuery.getUrgency() != -1) {
			predicates.add(
					builder.and(
							builder.equal(root.get(Todo_.URGENCY), todoQuery.getUrgency())));
		}
		
		if(!todoQuery.getDeadlineFrom().equals("")) {
			predicates.add(
					builder.and(
							builder.greaterThanOrEqualTo(root.get(Todo_.DEADLINE), Utils.str2date(todoQuery.getDeadlineFrom()))));
		}
		
		if(!todoQuery.getDeadlineTo().equals("")) {
			predicates.add(
					builder.and(
							builder.lessThanOrEqualTo(root.get(Todo_.DEADLINE), Utils.str2date(todoQuery.getDeadlineTo()))));
		}
		
		
		if(todoQuery.getDone() != null && todoQuery.getDone().equals("Y")) {
			predicates.add(
					builder.and(
							builder.equal(root.get(Todo_.DONE), todoQuery.getDone())));
		}
		
		
		Predicate[] predArray = new Predicate[predicates.size()];
		predicates.toArray(predArray);
		query = query.select(root).where(predArray).orderBy(builder.asc(root.get(Todo_.id)));
		
		TypedQuery<Todo> typedQuery = entityManager.createQuery(query);
		
		int totalRows = typedQuery.getResultList().size();
		typedQuery.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
		
		typedQuery.setMaxResults(pageable.getPageSize());
		
		Page<Todo> page = new PageImpl<Todo>(typedQuery.getResultList(), pageable, totalRows);
		return page;
	}

}
