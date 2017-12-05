#
# Copyright (C) 2017 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import logging
import re
from sre_constants import error as regex_error

from vts.runners.host import const
from vts.utils.python.common import list_utils

REGEX_PREFIX = 'r('
REGEX_SUFFIX = ')'
REGEX_PREFIX_ESCAPE = '\\r('
_INCLUDE_FILTER = '_include_filter'
_EXCLUDE_FILTER = '_exclude_filter'
DEFAULT_EXCLUDE_OVER_INCLUDE = False


def ExpandBitness(input_list):
    '''Expand filter items with bitness suffix.

    If a filter item contains bitness suffix, only test name with that tag will be included
    in output.
    Otherwise, both 32bit and 64bit suffix will be paired to the test name in output
    list.

    Args:
        input_list: list of string, the list to expand

    Returns:
        A list of string
    '''
    result = []
    for item in input_list:
        result.append(str(item))
        if (not item.endswith(const.SUFFIX_32BIT) and
                not item.endswith(const.SUFFIX_64BIT)):
            result.append("%s_%s" % (item, const.SUFFIX_32BIT))
            result.append("%s_%s" % (item, const.SUFFIX_64BIT))
    return list_utils.DeduplicateKeepOrder(result)


def SplitFilterList(input_list):
    '''Split filter items into exact and regex lists.

    To specify a regex filter, the syntax is:
      'suite.test' for exact matching
      'r(suite.test)' for regex matching of 'suite.test', where '.' means
          one of any char.
      '\r(suite.test)' for exact matching of name 'r(suite.test)', where
          '\r' is a two char string ('\\r' in code).

    Args:
        input_list: list of string, the list to explit

    Returns:
        A tuple of lists: two lists where the first one is exact matching
                          list and second one is regex list where the wrapping
                          syntax 'r(..)' is removed.
    '''
    exact = []
    regex = []
    for item in input_list:
        if item.startswith(REGEX_PREFIX) and item.endswith(REGEX_SUFFIX):
            regex_item = item[len(REGEX_PREFIX):-len(REGEX_SUFFIX)]
            try:
                re.compile(regex_item)
                regex.append(regex_item)
            except regex_error:
                logging.error('Invalid regex %s, ignored. Please refer to '
                              'python re syntax documentation.' % regex_item)
        elif item.startswith(REGEX_PREFIX_ESCAPE) and item.endswith(
                REGEX_SUFFIX):
            exact.append(REGEX_PREFIX + item[len(REGEX_PREFIX_ESCAPE):])
        else:
            exact.append(item)

    return (exact, regex)


def InRegexList(item, regex_list):
    '''Checks whether a given string matches an item in the given regex list.

    Args:
        item: string, given string
        regex_list: regex list

    Returns:
        bool, True if there is a match; False otherwise.
    '''
    for regex in regex_list:
        p = re.compile(regex)
        m = p.match(item)
        if m and m.start() == 0 and m.end() == len(item):
            return True

    return False


class Filter(object):
    '''A class to hold test filter rules and filter test names.

    Regex matching is supported. Regex syntax is python re package syntax.
    To specify a regex filter, the syntax is:
      'suite.test' for exact matching
      'r(suite.test)' for regex matching of 'suite.test', where '.' means
          one of any char.
      '\r(suite.test)' for exact matching of name 'r(suite.test)', where
          '\r' is a two char string ('\\r' in code).

    Attributes:
        is_enabled_regex: bool, whether regex is enabled.
        include_filter: list of string, input include filter
        exclude_filter: list of string, input exclude filter
        include_filter_exact: list of string, exact include filter
        include_filter_regex: list of string, exact include filter
        exclude_filter_exact: list of string, exact exclude filter
        exclude_filter_regex: list of string, exact exclude filter
        exclude_over_include: bool, False for include over exclude;
                              True for exclude over include.
    '''
    include_filter_exact = []
    include_filter_regex = []
    exclude_filter_exact = []
    exclude_filter_regex = []

    def __init__(self,
                 include_filter=[],
                 exclude_filter=[],
                 enable_regex=False,
                 exclude_over_include=None):
        self.is_enabled_regex = enable_regex
        self.include_filter = include_filter
        self.exclude_filter = exclude_filter
        if exclude_over_include is None:
            exclude_over_include = DEFAULT_EXCLUDE_OVER_INCLUDE
        self.exclude_over_include = exclude_over_include

    def ExpandBitness(self):
        '''Expand bitness from filter.

        Items in the filter that doesn't contain bitness suffix will be expended
        to 3 items, 2 of which ending with bitness. This method is safe if
        called multiple times. Regex items will not be expanded
        '''
        self.include_filter_exact = ExpandBitness(self.include_filter_exact)
        self.exclude_filter_exact = ExpandBitness(self.exclude_filter_exact)

    def Filter(self, item):
        '''Filter a given string using the internal filters.

        Rule:
            If include_filter is empty, only exclude_filter is checked
            for non-passing. Otherwise, only include_filter is checked
            (include_filter overrides exclude_filter).

        Args:
            item: string, the string for filter check

        Returns:
            bool. True if it passed the filter; False otherwise
        '''
        if not self.exclude_over_include:
            return (self.IsInIncludeFilter(item) if self.include_filter else
                    not self.IsInExcludeFilter(item))

        if self.IsInExcludeFilter(item):
            return False

        if self.include_filter:
            return self.IsInIncludeFilter(item)

    def IsInIncludeFilter(self, item):
        '''Check if item is in include filter.'''
        return item in self.include_filter_exact or InRegexList(
            item, self.include_filter_regex)

    def IsInExcludeFilter(self, item):
        '''Check if item is in exclude filter.'''
        return item in self.exclude_filter_exact or InRegexList(
            item, self.exclude_filter_regex)

    @property
    def include_filter(self):
        '''Getter method for include_filter'''
        return getattr(self, _INCLUDE_FILTER, [])

    @include_filter.setter
    def include_filter(self, include_filter):
        '''Setter method for include_filter'''
        setattr(self, _INCLUDE_FILTER, include_filter)
        if self.is_enabled_regex:
            self.include_filter_exact, self.include_filter_regex = SplitFilterList(
                include_filter)
        else:
            self.include_filter_exact = include_filter

    @property
    def exclude_filter(self):
        '''Getter method for exclude_filter'''
        return getattr(self, _EXCLUDE_FILTER, [])

    @exclude_filter.setter
    def exclude_filter(self, exclude_filter):
        '''Setter method for exclude_filter'''
        setattr(self, _EXCLUDE_FILTER, exclude_filter)
        if self.is_enabled_regex:
            self.exclude_filter_exact, self.exclude_filter_regex = SplitFilterList(
                exclude_filter)
        else:
            self.exclude_filter_exact = exclude_filter

    def __str__(self):
        return ('Filter:\nis_enabled_regex: {is_enabled_regex}\n'
                'include_filter: {include_filter}\n'
                'exclude_filter: {exclude_filter}\n'
                'include_filter_exact: {include_filter_exact}\n'
                'include_filter_regex: {include_filter_regex}\n'
                'exclude_filter_exact: {exclude_filter_exact}\n'
                'exclude_filter_regex: {exclude_filter_regex}'.format(
                    is_enabled_regex=self.is_enabled_regex,
                    include_filter=self.include_filter,
                    exclude_filter=self.exclude_filter,
                    include_filter_exact=self.include_filter_exact,
                    include_filter_regex=self.include_filter_regex,
                    exclude_filter_exact=self.exclude_filter_exact,
                    exclude_filter_regex=self.exclude_filter_regex))
