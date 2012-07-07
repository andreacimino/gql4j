package com.googlecode.gql4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.Date;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Query.SortPredicate;
import com.google.appengine.repackaged.com.google.common.collect.ImmutableList;
import com.google.appengine.repackaged.com.google.common.collect.ImmutableMap;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.gql4j.GqlQuery.BooleanEvaluator;
import com.googlecode.gql4j.GqlQuery.Condition;
import com.googlecode.gql4j.GqlQuery.DecimalEvaluator;
import com.googlecode.gql4j.GqlQuery.Evaluator;
import com.googlecode.gql4j.GqlQuery.From;
import com.googlecode.gql4j.GqlQuery.FunctionEvaluator;
import com.googlecode.gql4j.GqlQuery.Limit;
import com.googlecode.gql4j.GqlQuery.ListEvaluator;
import com.googlecode.gql4j.GqlQuery.NullEvaluator;
import com.googlecode.gql4j.GqlQuery.Offset;
import com.googlecode.gql4j.GqlQuery.OrderBy;
import com.googlecode.gql4j.GqlQuery.OrderByItem;
import com.googlecode.gql4j.GqlQuery.ParamEvaluator;
import com.googlecode.gql4j.GqlQuery.ParseResult;
import com.googlecode.gql4j.GqlQuery.Select;
import com.googlecode.gql4j.GqlQuery.StringEvaluator;
import com.googlecode.gql4j.GqlQuery.Where;
import com.googlecode.gql4j.antlr.GQLLexer;
import com.googlecode.gql4j.antlr.GQLParser;

/**
 * @author Max Zhu (thebbsky@gmail.com)
 *
 */
public class GqlQueryTest {

	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

	@Before
	public void setUp() {
		helper.setUp();
	}

	@After
	public void tearDown() {
		helper.tearDown();
	}
	
	//==================lexer and parser test=================
	public GQLParser parser(String inputStr) {
		CharStream input = new ANTLRStringStream(inputStr);
		
		GQLLexer lexer = new GQLLexer(input);
		TokenStream tokens = new CommonTokenStream(lexer);
		GQLParser parser = new GQLParser(tokens);
		return parser;
	}
	
	@Test
	public void testValue_1() throws RecognitionException {		
		assertEquals(NullEvaluator.get(), parser("Null").value().r);
		assertEquals(new StringEvaluator("'abc'"), parser("'abc'").value().r);
		assertEquals(new BooleanEvaluator("true"), parser("true").value().r);
		assertEquals(new ParamEvaluator("abc"), parser(":abc").value().r);
		assertEquals(
				new FunctionEvaluator("key", 
						ImmutableList.<Evaluator>of(new StringEvaluator("'abc'"))), 
				parser("key('abc')").value().r);
		
		assertEquals(new DecimalEvaluator("1"), parser("1").value().r);
	}
	
	@Test
	public void testCondition_1() throws RecognitionException {
		Condition expected = new Condition("a", FilterOperator.EQUAL, new DecimalEvaluator("1"));		
		Condition actual = parser("a=1").condition().r;
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testCondition_2() throws RecognitionException {
		Condition expected = new Condition("d", FilterOperator.EQUAL, 
				new FunctionEvaluator("datetime", new StringEvaluator("'2000-2-2 3:4:5'")));		

		Condition actual = parser("d = datetime('2000-2-2 3:4:5') ").condition().r;
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testFunctionDatetime_1() throws RecognitionException {
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(Calendar.YEAR, 2000);
		c.set(Calendar.MONTH, 1);
		c.set(Calendar.DATE, 2);
		c.set(Calendar.HOUR, 3);
		c.set(Calendar.MINUTE, 4);
		c.set(Calendar.SECOND, 5);
		Date expected = c.getTime();
		
		assertEquals(expected, parser("datetime(2000, 1, 2, 3, 4, 5)").value().r.evaluate(null));
	}
	
	@Test
	public void testFunctionDatetime_2() throws RecognitionException {
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(Calendar.YEAR, 2000);
		c.set(Calendar.MONTH, 1);
		c.set(Calendar.DATE, 2);
		c.set(Calendar.HOUR, 3);
		c.set(Calendar.MINUTE, 4);
		c.set(Calendar.SECOND, 5);
		Date expected = c.getTime();
		
		assertEquals(expected, parser("datetime('2000-2-2 3:4:5')").value().r.evaluate(null));
	}
	
	@Test
	public void testFunctionDate_1() throws RecognitionException {
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(Calendar.YEAR, 2000);
		c.set(Calendar.MONTH, 1);
		c.set(Calendar.DATE, 2);
		Date expected = c.getTime();
		
		assertEquals(expected, parser("date(2000, 1, 2)").value().r.evaluate(null));
	}
	
	@Test
	public void testFunctionDate_2() throws RecognitionException {
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(Calendar.YEAR, 2000);
		c.set(Calendar.MONTH, 1);
		c.set(Calendar.DATE, 2);
		Date expected = c.getTime();
		
		assertEquals(expected, parser("date('2000-2-2')").value().r.evaluate(null));
	}
	
	@Test
	public void testKey_1() throws RecognitionException {
		Key actual = 
			(Key) parser("key('ahhzfnlldGFub3RoZXJhZG1pbmNvbnNvbGVydwsSFktJTkRfV0lUSF9BTExfUFJPUEVSVFkYZAwLEhZLSU5EX1dJVEhfQUxMX1BST1BFUlRZIhlzdHJpbmdfa2V5X3dpdGhvdXRfZW50aXR5DAsSFktJTkRfV0lUSF9BTExfUFJPUEVSVFkiCnN0cmluZ19rZXkM')").value().r.evaluate(null);
		
		assertEquals("KIND_WITH_ALL_PROPERTY", actual.getKind());
		assertEquals("KIND_WITH_ALL_PROPERTY", actual.getParent().getKind());
		assertEquals("KIND_WITH_ALL_PROPERTY", actual.getParent().getParent().getKind());
		assertNull(actual.getParent().getParent().getParent());
		
		assertEquals("string_key", actual.getName());
		assertEquals("string_key_without_entity", actual.getParent().getName());
		assertEquals(100, actual.getParent().getParent().getId());
	}
	
	@Test
	public void testKey_2() throws RecognitionException {
		Key actual = 
			(Key) parser("key('KIND_WITH_ALL_PROPERTY', 100, 'KIND_WITH_ALL_PROPERTY', 'string_key_without_entity', 'KIND_WITH_ALL_PROPERTY', 'string_key')"
					).value().r.evaluate(null);
		
		assertEquals("KIND_WITH_ALL_PROPERTY", actual.getKind());
		assertEquals("KIND_WITH_ALL_PROPERTY", actual.getParent().getKind());
		assertEquals("KIND_WITH_ALL_PROPERTY", actual.getParent().getParent().getKind());
		assertNull(actual.getParent().getParent().getParent());
		
		assertEquals("string_key", actual.getName());
		assertEquals("string_key_without_entity", actual.getParent().getName());
		assertEquals(100, actual.getParent().getParent().getId());
	}
	
	//==================overall parsing test=================
	@Test
	public void testParseSelect_1() {
		ParseResult actual = GqlQuery.parse("SELECT __key__");
		ParseResult expected = new ParseResult().setSelect(new Select(true));
		assertEquals(expected, actual);
	}
	
	@Test
	public void testParseSelect_2() {
		ParseResult actual = GqlQuery.parse("SELECT *");
		ParseResult expected = new ParseResult().setSelect(new Select(false));
		assertEquals(expected, actual);
	}
	
	@Test
	public void testParseFrom_1() {
		ParseResult actual = GqlQuery.parse("SELECT * from a");
		ParseResult expected = new ParseResult().setSelect(new Select(false)).setFrom(new From("a"));
		assertEquals(expected, actual);
	}
	
	@Test
	public void testParseWhere_1() {
		ParseResult actual = GqlQuery.parse("SELECT * from a where a = 1");
		ParseResult expected = new ParseResult().setSelect(new Select(false)).setFrom(new From("a")).setWhere(
				new Where().withCondition(new Condition("a", FilterOperator.EQUAL, new DecimalEvaluator("1"))));
		assertEquals(expected, actual);
	}
	
	@Test
	public void testParseWhere_2() {
		ParseResult actual = GqlQuery.parse("SELECT * from a where a = 1 and b <= '3'");
		ParseResult expected = new ParseResult().setSelect(new Select(false)).setFrom(new From("a")).setWhere(
				new Where().withCondition(new Condition("a", FilterOperator.EQUAL, new DecimalEvaluator("1")))
				.withCondition(new Condition("b", FilterOperator.LESS_THAN_OR_EQUAL, new StringEvaluator("'3'")))
				);
		assertEquals(expected, actual);
	}
	
	@Test
	public void testParseWhere_3() {
		ParseResult actual = GqlQuery.parse("SELECT * from a where a = :1");
		ParseResult expected = new ParseResult().setSelect(new Select(false)).setFrom(new From("a")).setWhere(
				new Where().withCondition(new Condition("a", FilterOperator.EQUAL, new ParamEvaluator("1"))));
		assertEquals(expected, actual);
	}
	
	@Test
	public void testParseWhere_4() {
		ParseResult actual = GqlQuery.parse("SELECT * from a where a = :abc");
		ParseResult expected = new ParseResult().setSelect(new Select(false)).setFrom(new From("a")).setWhere(
				new Where().withCondition(new Condition("a", FilterOperator.EQUAL, new ParamEvaluator("abc"))));
		assertEquals(expected, actual);
	}
	
	@Test
	public void testParseWhere_5() {
		ParseResult actual = GqlQuery.parse("SELECT * from a where a in (:abc, 'b', 'c')");
		ParseResult expected = new ParseResult().setSelect(new Select(false)).setFrom(new From("a")).setWhere(
				new Where().withCondition(new Condition("a", FilterOperator.IN, 
						new ListEvaluator(new ParamEvaluator("abc"), new StringEvaluator("'b'"), new StringEvaluator("'c'")))));
		assertEquals(expected, actual);
	}
	
	@Test
	public void testParseWhere_6() {
		ParseResult actual = GqlQuery.parse("SELECT * from a WHERE ANCESTOR IS KEY('Person', 'Amy')");
		ParseResult expected = new ParseResult().setSelect(new Select(false)).setFrom(new From("a")).setWhere(
				new Where().withAncestor(
						new FunctionEvaluator("key", new StringEvaluator("'Person'"), new StringEvaluator("'Amy'"))));
		assertEquals(expected, actual);
	}
	
	@Test
	public void testOrderBy_1() {
		ParseResult actual = GqlQuery.parse("SELECT * from a order by a desc");
		ParseResult expected = new ParseResult().setSelect(new Select(false)).setFrom(new From("a")).setOrderBy(
				new OrderBy().withItem(new OrderByItem("a").setDirection(false)));
		assertEquals(expected, actual);
	}
	
	@Test
	public void testOrderBy_2() {
		ParseResult actual = GqlQuery.parse("SELECT * from a order by a desc, b, c asc");
		ParseResult expected = new ParseResult().setSelect(new Select(false)).setFrom(new From("a")).setOrderBy(
				new OrderBy().withItem(new OrderByItem("a").setDirection(false))
				.withItem(new OrderByItem("b").setDirection(true))
				.withItem(new OrderByItem("c").setDirection(true)));
		assertEquals(expected, actual);
	}
	
	@Test
	public void testLimit_1() {
		ParseResult actual = GqlQuery.parse("SELECT * from a limit 5");
		ParseResult expected = new ParseResult().setSelect(new Select(false)).setFrom(new From("a"
				)).setLimit(new Limit(5));
		assertEquals(expected, actual);
	}
	
	@Test
	public void testOffset_1() {
		ParseResult actual = GqlQuery.parse("SELECT * from a limit 5 offset 10");
		ParseResult expected = new ParseResult().setSelect(new Select(false)).setFrom(new From("a"
				)).setLimit(new Limit(5)).setOffset(new Offset(10));
		assertEquals(expected, actual);
	}
	
	// ==============================integrated test=============================
	@SuppressWarnings("deprecation")
	@Test
	public void test1() {
		GqlQuery gql = new GqlQuery(
				"SELECT __key__ from a where b = :1 and c < :2 and d = datetime('2011-11-17 10:10:10') " +
				"order by d asc, e, f desc limit 1000 offset 200", 
				"param1_val", 100);
		
		Query actual = gql.query();

		// where clause
		assertEquals(3, actual.getFilterPredicates().size());
		assertEquals(new FilterPredicate("b", FilterOperator.EQUAL, "param1_val"), actual.getFilterPredicates().get(0));
		assertEquals(new FilterPredicate("c", FilterOperator.LESS_THAN, 100), actual.getFilterPredicates().get(1));
		assertEquals(new FilterPredicate("d", FilterOperator.EQUAL, new Date(2011 - 1900, 10, 17, 10, 10, 10)), actual.getFilterPredicates().get(2));
		
		// order by 
		assertEquals(3, actual.getSortPredicates().size());
		assertEquals(new SortPredicate("d", SortDirection.ASCENDING), actual.getSortPredicates().get(0));
		assertEquals(new SortPredicate("e", SortDirection.ASCENDING), actual.getSortPredicates().get(1));
		assertEquals(new SortPredicate("f", SortDirection.DESCENDING), actual.getSortPredicates().get(2));
		
		// limit 
		assertEquals(new Integer(1000), gql.fetchOptions().getLimit());
		
		// offset
		assertEquals(new Integer(200), gql.fetchOptions().getOffset());
	}
	
	public void test2() {
		GqlQuery gql = new GqlQuery(
				"SELECT * where ANCESTOR IS KEY(:kind, :name)", 
				ImmutableMap.<String, Object>of("kind", "kind_A", "name", "Peter"));
		
		Query actual = gql.query();
		
		assertNull(actual.getKind());
		assertNull(actual.getFilterPredicates());
		assertNull(actual.getSortPredicates());
		
		Key ancestor = actual.getAncestor();
		assertEquals("kind_A", ancestor.getKind());
		assertEquals("Peter", ancestor.getName());
	}
}
