package net.ali.api.filters

import javax.inject.Inject
import play.api.http.{HttpFilters => PlayHttpFilters}

// This class lists all the filters we want to use.
// In our case, only one. Play knows where to look
// for the filters thanks to the configuration
// variable play.http.filters in application.conf
class HttpFilters @Inject()(
  accessLogFilter: AccessLogFilter
) extends PlayHttpFilters {

  def filters = Seq(accessLogFilter)

}
