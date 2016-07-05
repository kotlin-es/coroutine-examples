package es.kotlin.db.async.mysql

import es.kotlin.crypto.Hash

// Converted from my .NET code at: https://github.com/soywiz/NodeNetAsync/blob/master/NodeNetAsync/Db/Mysql/

internal object MysqlAuth {
	fun Token(password: ByteArray?, scrambleBuffer: ByteArray): ByteArray {
		if (password == null || password.size === 0) return ByteArray(0)
		val Stage1 = sha1(password)
		val Stage2 = sha1(Stage1)
		val Stage3 = sha1(scrambleBuffer + Stage2)
		return xor(Stage3, Stage1)
	}

	private fun sha1(data: ByteArray): ByteArray = Hash.SHA1.hash(data)

	private fun xor(left: ByteArray, right: ByteArray): ByteArray {
		val out = ByteArray(left.size)
		for (n in 0 until out.size) out[n] = (left[n].toInt() xor right[n].toInt()).toByte()
		return out
	}
}

class MysqlException(val errorCode: Int, val sqlState: String, message: String) : RuntimeException(message)

class MysqlField(
	val Catalog: String?,
	val Database: String?,
	val Table: String?,
	val OrgTable: String?,
	val Name: String?,
	val OrgName: String?,
	val Unk1: Int,
	val Charset: Int,
	val Length: Long,
	val Type: MysqlFieldTypeEnum,
	val Flags: MysqlFieldFlagsSet,
	val Decimals: Byte,
	val Unk2: Byte,
	val Default: ByteArray
)

class MysqlColumns : Iterable<MysqlField> {
	private val columnsByIndex = arrayListOf<MysqlField>()
	private val columnsByName = hashMapOf<String?, MysqlField>()
	private val columnIndexByName = hashMapOf<String?, Int>()

	fun add(Column: MysqlField) {
		columnsByIndex += Column
		columnsByName[Column.Name] = Column
		columnIndexByName[Column.Name] = columnsByIndex.size - 1
	}

	operator fun get(index: Int) = columnsByIndex[index]
	operator fun get(name: String) = columnsByName[name]

	fun getIndexByColumnName(Name: String): Int = columnIndexByName[Name]!!

	val length: Int get() = columnsByIndex.size

	override fun iterator(): Iterator<MysqlField> = columnsByIndex.iterator()
}

class MysqlQueryResult : Iterable<MysqlRow> {
	val columns = MysqlColumns()
	val rows = arrayListOf<MysqlRow>()

	override fun iterator(): Iterator<MysqlRow> = rows.iterator()
}

//class MysqlRow : IEnumerable<KeyValuePair<string, string>>, IDictionary {
class MysqlRow(val columns: MysqlColumns) {
	val Cells = arrayListOf<String>()

	operator fun get(name: String) = this[columns.getIndexByColumnName(name)]
	operator fun get(index: Int) = Cells[index]

	//public fun CastTo<TType>(): TType
	//{
	//	var ItemValue = (TType)Activator.CreateInstance(typeof(TType));
	//	var ItemType = typeof(TType);
//
	//	for (int n = 0; n < Columns.Length; n++)
	//	{
	//		var Column = Columns[n];
	//		var Value = Cells[n];
	//		var Field = ItemType.GetField(Column.Name);
	//		if (Field != null)
	//		{
	//			object ValueObject = null;
//
	//			if (Field.FieldType == typeof(bool))
	//			{
	//				ValueObject = (int.Parse(Value) != 0);
	//			}
	//			else if (Field.FieldType == typeof(int))
	//			{
	//				ValueObject = int.Parse(Value);
	//			}
	//			else if (Field.FieldType == typeof(string))
	//			{
	//				ValueObject = Value;
	//			}
	//			else
	//			{
	//				throw(new NotImplementedException("Can't handle type '" + Field.FieldType + "'"));
	//			}
//
	//			Field.SetValueDirect(__makeref(ItemValue), ValueObject);
	//		}
	//	}
//
	//	return ItemValue;
	//}

	//public override string ToString()
	//{
	//	var Parts = new List<string>();
	//	for (int n = 0; n < Cells.Count; n++)
	//	{
	//		Parts.Add("'" + Columns[n].Name + "': '" + Cells[n] + "'");
	//	}
	//	return "MysqlRow(" + String.Join(", ", Parts) + ")";
	//}
//
	//IEnumerator<KeyValuePair<string, string>> IEnumerable<KeyValuePair<string, string>>.GetEnumerator()
	//{
	//	for (int n = 0; n < Columns.Length; n++)
	//	{
	//		var Column = Columns[n];
	//		var Value = Cells[n];
	//		yield return new KeyValuePair<string, string>(Column.Name, Value);
	//	}
	//}
//
	//IEnumerator IEnumerable.GetEnumerator()
	//{
	//	foreach (var Item in ((IEnumerable<KeyValuePair<string, string>>)this).AsEnumerable()) yield return Item;
	//}
//
	//IDictionaryEnumerator IDictionary.GetEnumerator()
	//{
	//	return new Enumerator(this);
	//}
}

