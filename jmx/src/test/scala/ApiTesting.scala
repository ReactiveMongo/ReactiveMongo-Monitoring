package reactivemongo

import reactivemongo.api.MongoConnection

package object tests {
  @inline def name(con: MongoConnection): String = con.name
}
