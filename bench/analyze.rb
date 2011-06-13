#!/usr/bin/env ruby

if ARGV.length < 1
  puts "Usage: #{$0} FILE_TO_ANALYZE [ANOTHER_FILE]"
  exit 1
end

require "rubygems"
require "bundler/setup"

require "nokogiri"
require "statarray"

ARGV.each do |file|
  contents = File.read(file)
  cleaned = contents.gsub(/<\?.*?\?>/, '')
  string = "<root>#{cleaned}</root>"
  doc = Nokogiri::XML.parse(string)
  timings = StatArray.new(doc.xpath("//int[@name='QTime']").map(&:text).map(&:to_i))
  puts file + ":"
  puts "Mean:   #{timings.mean}"
  puts "Median: #{timings.median}"
  puts "Stddev: #{timings.stddev}"
end