package org.tribbloid.spookystuff.actions

import org.tribbloid.spookystuff.SpookyEnvSuite
import org.tribbloid.spookystuff.dsl._
import org.tribbloid.spookystuff.pages.Page
import org.tribbloid.spookystuff.session.Session

/**
 * Created by peng on 05/06/14.
 */

//TODO: this need some serious reorganization
class TestTrace extends SpookyEnvSuite {

  import scala.concurrent.duration._

  test("resolve") {
    val results = Trace(
      Visit("http://www.wikipedia.org") ::
        WaitFor("input#searchInput").in(40.seconds) ::
        Snapshot().as('A) ::
        TextInput("input#searchInput","Deep learning") ::
        Submit("input.formBtn") ::
        Snapshot().as('B) :: Nil
    ).resolve(spooky)

    val resultsList = results
    assert(resultsList.length === 2)
    val res1 = resultsList(0).asInstanceOf[Page]
    val res2 = resultsList(1).asInstanceOf[Page]

    val id1 = Trace(Visit("http://www.wikipedia.org")::WaitFor("input#searchInput")::Snapshot().as('C)::Nil)
    assert(res1.uid.backtrace === id1)
    assert(res1.markup.get.contains("<title>Wikipedia</title>"))
    assert(res1.uri === "http://www.wikipedia.org/")
    assert(res1.name === "A")

    val id2 = Trace(Visit("http://www.wikipedia.org")::WaitFor("input#searchInput")::TextInput("input#searchInput","Deep learning")::Submit("input.formBtn")::Snapshot().as('D)::Nil)
    assert(res2.uid.backtrace === id2)
    assert(res2.markup.get.contains("<title>Deep learning - Wikipedia, the free encyclopedia</title>"))
    assert(res2.uri === "http://en.wikipedia.org/wiki/Deep_learning")
    assert(res2.name === "B")
  }

//  test("cache multiple pages and restore") {
//    val results = PageBuilder.resolve(
//      Visit("https://www.linkedin.com/") ::
//        Snapshot().as("T") :: Nil,
//      dead = false
//    )(spooky)
//
//    val resultsList = results.toArray
//    assert(resultsList.size === 1)
//    val page1 = resultsList(0)
//
//    val page1Cached = page1.autoCache(spooky,overwrite = true)
//
//    val uid = PageUID( Visit("https://www.linkedin.com/") :: Snapshot() :: Nil)
//
//    val cachedPath = new Path(
//      Utils.urlConcat(
//        spooky.autoCacheRoot,
//        spooky.PagePathLookup(uid).toString
//      )
//    )
//
//    val loadedPage = PageBuilder.restoreLatest(cachedPath)(spooky.hConf)(0)
//
//    assert(page1Cached.content === loadedPage.content)
//
//    assert(page1Cached.copy(content = null) === loadedPage.copy(content = null))
//  }
}
