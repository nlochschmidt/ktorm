package me.liuwj.ktorm.entity

import me.liuwj.ktorm.BaseTest
import me.liuwj.ktorm.dsl.*
import me.liuwj.ktorm.schema.*
import org.junit.Test
import java.time.LocalDate

/**
 * Created by vince on Aug 10, 2019.
 */
class DataClassTest : BaseTest() {

    data class Section(
        val id: Int,
        val name: String,
        val location: String
    )

    data class Staff(
        val id: Int,
        val name: String,
        val job: String,
        val managerId: Int,
        val hireDate: LocalDate,
        val salary: Long,
        val sectionId: Int
    )

    object Sections : BaseTable<Section>("t_department") {
        val id = int("id").primaryKey()
        val name = varchar("name")
        val location = varchar("location")

        override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Section(
            id = row[id] ?: 0,
            name = row[name].orEmpty(),
            location = row[location].orEmpty()
        )
    }

    object Staffs : BaseTable<Staff>("t_employee") {
        val id = int("id").primaryKey()
        val name = varchar("name")
        val job = varchar("job")
        val managerId = int("manager_id")
        val hireDate = date("hire_date")
        val salary = long("salary")
        val sectionId = int("department_id")

        override fun doCreateEntity(row: QueryRowSet, withReferences: Boolean) = Staff(
            id = row[id] ?: 0,
            name = row[name].orEmpty(),
            job = row[job].orEmpty(),
            managerId = row[managerId] ?: 0,
            hireDate = row[hireDate] ?: LocalDate.now(),
            salary = row[salary] ?: 0,
            sectionId = row[sectionId] ?: 0
        )
    }

    @Test
    fun testFindById() {
        val staff = database.sequenceOf(Staffs).find { it.id eq 1 } ?: throw AssertionError()
        assert(staff.name == "vince")
        assert(staff.job == "engineer")
    }

    @Test
    fun testFindList() {
        val staffs = database.sequenceOf(Staffs).filter { it.sectionId eq 1 }.toList()
        assert(staffs.size == 2)
        assert(staffs.mapTo(HashSet()) { it.name } == setOf("vince", "marry"))
    }

    @Test
    fun testSelectName() {
        val staffs = database
            .from(Staffs)
            .select(Staffs.name)
            .where { Staffs.id eq 1 }
            .map { Staffs.createEntity(it) }
        assert(staffs[0].name == "vince")
    }

    @Test
    fun testJoin() {
        val staffs = database
            .from(Staffs)
            .leftJoin(Sections, on = Staffs.sectionId eq Sections.id)
            .select(Staffs.columns)
            .where { Sections.location like "%Guangzhou%" }
            .orderBy(Staffs.id.asc())
            .map { Staffs.createEntity(it) }

        assert(staffs.size == 2)
        assert(staffs[0].name == "vince")
        assert(staffs[1].name == "marry")
    }

    @Test
    fun testSequence() {
        val staffs = database
            .sequenceOf(Staffs)
            .filter { it.sectionId eq 1 }
            .sortedBy { it.id }
            .toList()

        assert(staffs.size == 2)
        assert(staffs[0].name == "vince")
        assert(staffs[1].name == "marry")
    }

    @Test
    fun testCount() {
        assert(database.sequenceOf(Staffs).count { it.sectionId eq 1 } == 2)
    }

    @Test
    fun testFold() {
        val totalSalary = database.sequenceOf(Staffs).fold(0L) { acc, staff -> acc + staff.salary }
        assert(totalSalary == 450L)
    }

    @Test
    fun testGroupingBy() {
        val salaries = database
            .sequenceOf(Staffs)
            .groupingBy { it.sectionId * 2 }
            .fold(0L) { acc, staff ->
                acc + staff.salary
            }

        println(salaries)
        assert(salaries.size == 2)
        assert(salaries[2] == 150L)
        assert(salaries[4] == 300L)
    }

    @Test
    fun testEachCount() {
        val counts = database
            .sequenceOf(Staffs)
            .filter { it.salary less 100000L }
            .groupingBy { it.sectionId }
            .eachCount()

        println(counts)
        assert(counts.size == 2)
        assert(counts[1] == 2)
        assert(counts[2] == 2)
    }

    @Test
    fun testMapColumns() {
        val (name, job) = database
            .sequenceOf(Staffs)
            .filter { it.sectionId eq 1 }
            .filterNot { it.managerId.isNotNull() }
            .mapColumns2 { tupleOf(it.name, it.job) }
            .single()

        assert(name == "vince")
        assert(job == "engineer")
    }

    @Test
    fun testGroupingAggregate() {
        database
            .sequenceOf(Staffs)
            .groupingBy { it.sectionId }
            .aggregateColumns2 { tupleOf(max(it.salary), min(it.salary)) }
            .forEach { sectionId, (max, min) ->
                println("$sectionId:$max:$min")
            }
    }
}
