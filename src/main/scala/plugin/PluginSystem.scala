package plugin

import app.Context
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import javax.script.ScriptEngineManager
import scala.collection.mutable.ListBuffer
import org.slf4j.LoggerFactory
import jdk.nashorn.api.scripting.ScriptObjectMirror

/**
 * Provides extension points to plug-ins.
 */
object PluginSystem {

  private val logger = LoggerFactory.getLogger(PluginSystem.getClass)

  private val repositoryMenuList   = ListBuffer[RepositoryMenu]()
  private val globalMenuList       = ListBuffer[GlobalMenu]()
  private val repositoryActionList = ListBuffer[Action]()
  private val globalActionList     = ListBuffer[Action]()

  case class GlobalMenu(label: String, url: String, icon: String, condition: Context => Boolean)
  case class RepositoryMenu(label: String, name: String, url: String, icon: String, condition: Context => Boolean)
  case class Action(path: String, function: (HttpServletRequest, HttpServletResponse) => Any)

  def addRepositoryMenu(label: String, name: String, url: String, icon: String)(condition: Context => Boolean): Unit = {
    repositoryMenuList += RepositoryMenu(label, name, url, icon, condition)
  }

  def addRepositoryMenu(label: String, name: String, url: String, icon: String, condition: ScriptObjectMirror): Unit = {
    repositoryMenuList += RepositoryMenu(label, name, url, icon, (context) => condition.call(this, context).asInstanceOf[Boolean])
  }

  def addGlobalMenu(label: String, url: String, icon: String)(condition: Context => Boolean): Unit = {
    globalMenuList += GlobalMenu(label, url, icon, condition)
  }

  def addGlobalMenu(label: String, url: String, icon: String, condition: ScriptObjectMirror): Unit = {
    globalMenuList += GlobalMenu(label, url, icon, (context) => condition.call(this, context).asInstanceOf[Boolean])
  }

  def addGlobalAction(path: String)(function: (HttpServletRequest, HttpServletResponse) => Any): Unit = {
    globalActionList += Action(path, function)
  }

  def addGlobalAction(path: String, function: ScriptObjectMirror): Unit = {
    globalActionList += Action(path, (request, response) => function.call(this, request, response))
  }

  def addRepositoryAction(path: String)(function: (HttpServletRequest, HttpServletResponse) => Any): Unit = {
    repositoryActionList += Action(path, function)
  }

  def addRepositoryAction(path: String, function: ScriptObjectMirror): Unit = {
    repositoryActionList += Action(path, (request, response) => function.call(this, request, response))
  }

  def evaluateJavaScript(script: String): Any = {
    val engine = new ScriptEngineManager().getEngineByName("JavaScript")
    logger.debug("Script: " + script)
    engine.put("PluginSystem", this)
    val result = engine.eval(script)
    logger.debug("Result: " + result)
    result
  }

  def repositoryMenus: List[RepositoryMenu] = repositoryMenuList.toList
  def globalMenus: List[GlobalMenu] = globalMenuList.toList
  def repositoryActions: List[Action] = repositoryActionList.toList
  def globalActions: List[Action] = globalActionList.toList

  // TODO This is a test
  addGlobalMenu("Google", "http://www.google.co.jp/", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAEvwAABL8BkeKJvAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAIgSURBVEiJtdZNiI1hFAfw36ORhSFFPgYLszOKJAsWRLGzks1gYyFZKFs7C7K2Y2XDRiwmq9kIJWQjJR9Tk48xRtTIRwjH4p473nm99yLNqdNTz/mf//+555x7ektEmEmbNaPs6OkUKKX0YBmWp6/IE8bwIs8xjEfEt0aiiJBl6sEuXMRLfEf8pX/PnIvJ0TPFWxE4+w+Ef/Kzbd5qDx5l8H8tkku7LG17gH7sxWatevdhEUoXsjda5RnDTZzH6jagtMe0lHIa23AJw3iOiSRZlmJ9mfcyfTzFl2AldmI3rkbEkbrAYKrX7S1eVRyWVnxhQ87eiLjQ+o2/mtyve+PuYy3W4+EfsP2/TVGKTHRI+Iz9Fdx8XOmAnZjGWRMYqoF/4ESW4hpOYk1iZ2WsLjDUTeBYBfgeuyux2XiNT5hXud+DD5W8Y90EtifoSfultfjx7MVtrKzcr8No5m7vJtCLx1hQJ8/4IZzClpyoy5ibsYUYQW81Z9o2jYgPeKr15+poEXE9+1XF9WIkOaasaV2P4k4pZUdDbEm+VEQcjIgtEfGxlLIVd/Gs6TX1MhzQquU3HK1t23f4IsuS94fxNXMO/MbXIDBg+tidw5yMbcCmylSdqWEH/kagYLKWeAt9Fcxi3KhhJuXq6SqQBMO15NDalvswmLWux4cbuToIbMS9BpJOfg8bm7imtmmTlVJWaa3hpnU9nufziBjtyDHTny0/AaA7Qnb4AM4aAAAAAElFTkSuQmCC")
    { context => context.loginAccount.isDefined }

  addRepositoryMenu("Board", "board", "/board", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAEvwAABL8BkeKJvAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAAIgSURBVEiJtdZNiI1hFAfw36ORhSFFPgYLszOKJAsWRLGzks1gYyFZKFs7C7K2Y2XDRiwmq9kIJWQjJR9Tk48xRtTIRwjH4p473nm99yLNqdNTz/mf//+555x7ektEmEmbNaPs6OkUKKX0YBmWp6/IE8bwIs8xjEfEt0aiiJBl6sEuXMRLfEf8pX/PnIvJ0TPFWxE4+w+Ef/Kzbd5qDx5l8H8tkku7LG17gH7sxWatevdhEUoXsjda5RnDTZzH6jagtMe0lHIa23AJw3iOiSRZlmJ9mfcyfTzFl2AldmI3rkbEkbrAYKrX7S1eVRyWVnxhQ87eiLjQ+o2/mtyve+PuYy3W4+EfsP2/TVGKTHRI+Iz9Fdx8XOmAnZjGWRMYqoF/4ESW4hpOYk1iZ2WsLjDUTeBYBfgeuyux2XiNT5hXud+DD5W8Y90EtifoSfultfjx7MVtrKzcr8No5m7vJtCLx1hQJ8/4IZzClpyoy5ibsYUYQW81Z9o2jYgPeKr15+poEXE9+1XF9WIkOaasaV2P4k4pZUdDbEm+VEQcjIgtEfGxlLIVd/Gs6TX1MhzQquU3HK1t23f4IsuS94fxNXMO/MbXIDBg+tidw5yMbcCmylSdqWEH/kagYLKWeAt9Fcxi3KhhJuXq6SqQBMO15NDalvswmLWux4cbuToIbMS9BpJOfg8bm7imtmmTlVJWaa3hpnU9nufziBjtyDHTny0/AaA7Qnb4AM4aAAAAAElFTkSuQmCC")
    { context => true}

  addGlobalAction("/hello"){ (request, response) =>
    "Hello World!"
  }

}


