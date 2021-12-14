package com.agcurations.aggallerymanager;

public class F_Downloader {

}

/*
Readme:
Help by me:
-Open a cmd window.
-Navigate in cmd to the folder holding main.py.
-Run python main.py.
-There might be an error, such as "ChromeDriver only supports version xxxx". In that case,
  go and download an updated Chrome Driver
-When the program opens a Chrome window, type in username and password and login.
  Complete any Captcha tasks.
-Return to the command window and press enter to continue.
-Wait for the script to process all of the addresses listed in urls.txt.
-If there is an error right away, it may be due to modifications to the website layout,
  - the html has changed. You will need to find the failing location in the python
  code and correct it. Use -D, the debug switch, to assist with altering the code.

# fakku-downloader

Fakku-downloader - this is python script that allows download manga directly from fakku.net.

### The problem

*Fakku.net manga reader has a good protect from download.*

As far as I know, Manga reader first decodes the encrypted image and then displays it on the html canvas. This is done so tricky that I could not find a way to automate the downloading of canvas because the JS functions for this are blocked in the domain. Therefore, in order to download manga, you need to do some non-trivial actions manually. And this will have to be done separately for each page.

### The easiest solution

In my opinion, the simplest and fastest solution for downloading manga from fakku.net is to simply open it in a browser and save a screenshot of each page. Fakku-downloader automates this process in background using headless browser.

## How to launch
1) Download or clone this repository
2) Download [ChromeDriver](https://chromedriver.chromium.org/downloads) the same version as you Chrome Browser and move it in root folder.
(Rename it to **chromedriver.exe**)
3) Create **urls.txt** file in root folder and write into that urls of manga one by line
4) Install all requirements for script via run **install.bat** (for Windows) or run <code>pip install -r requirements.txt</code>
5) Open root folder in command line and run the command <code>python main.py</code>

## Some features
* Use option -w for set wait time between loading the pages. If quality of .png is bad, or program somewhere crush its can help.
* Use option -t for set timeout for loading first page.
* Use option -l and -p for write the login and password from fakku.net
* More option technical you can find via --help

---

## Working example

1. After downloading the repository, chromedriver and creating urls.txt file, root folder will be like this:
<p align="center">
	<img src="https://github.com/Witness-senpai/fakku-downloader/blob/master/readme_png/1.PNG" width="800">
</p>
2. Urls in urls.txt views like this:
<p align="center">
	<img src="https://github.com/Witness-senpai/fakku-downloader/blob/master/readme_png/2.PNG" width="800">
</p>
3. Write the command: python main.py
<p align="center">
	<img src="https://github.com/Witness-senpai/fakku-downloader/blob/master/readme_png/3.PNG" width="800">
</p>
4. If you launch program in first time, you need to login in opening browser and press enter in console. After that program save the cookies and will be use it in next runs in headless browser mode and skeep this step.
<p align="center">
	<img src="https://github.com/Witness-senpai/fakku-downloader/blob/master/readme_png/4.PNG" width="800">
</p>
5. Downloading process
<p align="center">
	<img src="https://github.com/Witness-senpai/fakku-downloader/blob/master/readme_png/5.PNG" width="800">
</p>
6. The program will create its own folder for each manga in urls.txt
<p align="center">
	<img src="https://github.com/Witness-senpai/fakku-downloader/blob/master/readme_png/6.PNG" width="800">
</p>
7. And inside in each folder you can see the manga pages in the most affordable quality as in a browser.
<p align="center">
	<img src="https://github.com/Witness-senpai/fakku-downloader/blob/master/readme_png/7.PNG" width="800">
</p>

## Extra: Download URLs from a Collection

If you have a collection that has the manga that you would like to download,
you can generate a **urls.txt** file that has all of its links.

Setup as above, and then call like this:

```bash
python main.py -z https://www.fakku.net/users/MY-USER-12345/collections/MY-COLLECTION
```

This will make a **urls.txt** file with the links, then run the program as normal
with this file as input.


*/


/*
Main.py:
import argparse
from pathlib import Path

from downloader import (
    FDownloader,
    program_exit,
    TIMEOUT,
    WAIT,
    URLS_FILE,
    DONE_FILE,
    COOKIES_FILE,
    ROOT_MANGA_DIR,
    MAX,
    URLS_FILE,
    DEBUG_USE_STORED_HTML
)


def main():
    argparser = argparse.ArgumentParser()
    argparser.add_argument(
        "-z",
        "--collection_url",
        type=str,
        default=None,
        help=f"Give a collection URL that will be parsed and loaded into urls.txt \
            The normal operations of downloading manga images will not happen while this \
            parameter is set. \
            By default -- None, process the urls.txt instead")
    argparser.add_argument(
        "-f",
        "--file_urls",
        type=str,
        default=URLS_FILE,
        help=f".txt file that contains list of urls for download \
            By default -- {URLS_FILE}")
    argparser.add_argument(
        "-d",
        "--done_file",
        type=str,
        default=DONE_FILE,
        help=f".txt file that contains list of urls that have been downloaded. \
            This is used to resume in the event that the process stops midway. \
            By default -- {DONE_FILE}")
    argparser.add_argument(
        "-c",
        "--cookies_file",
        type=str,
        default=COOKIES_FILE,
        help=f"Binary file that contains saved cookies for authentication. \
            By default -- {COOKIES_FILE}")
    argparser.add_argument(
        "-o",
        "--output_dir",
        type=str,
        default=ROOT_MANGA_DIR,
        help=f"The directory that will be used as the root of the output \
            By default -- {ROOT_MANGA_DIR}")
    argparser.add_argument(
        "-l",
        "--login",
        type=str,
        default=None,
        help="Login or email for authentication")
    argparser.add_argument(
        "-p",
        "--password",
        type=str,
        default=None,
        help="Password for authentication")
    argparser.add_argument(
        "-t",
        "--timeout",
        type=float,
        default=TIMEOUT,
        help=f"Timeout in seconds for loading first manga page. \
            Increase this argument if quality of pages is bad. By default -- {TIMEOUT} sec")
    argparser.add_argument(
        "-w",
        "--wait",
        type=float,
        default=WAIT,
        help=f"Wait time in seconds for pauses beetween downloading pages \
            Increase this argument if you become blocked. By default -- {WAIT} sec")
    argparser.add_argument(
        "-m",
        "--max",
        type=int,
        default=MAX,
        help=f"Max number of volumes to download at once \
            Set this argument if you become blocked. By default -- No limit")
    argparser.add_argument(
        "-D",
        "--debug_use_stored_html",
        action="store_true",
        default=DEBUG_USE_STORED_HTML,
        help=f"Use stored HTML in folder. For editing python code to gather data. \
            Run once without debug to save the HTML, then run with debug switch. \
            HTML will be saved/recalled in accordance with the first record in \
            urls.txt.")
    args = argparser.parse_args()

    file_urls = Path(args.file_urls)
    if args.collection_url:
        Path(args.file_urls).touch()
    elif not file_urls.is_file() or file_urls.stat().st_size == 0:
        print(f'File {args.file_urls} does not exist or empty.\n' + \
            'Create it and write the list of manga urls first.\n' + \
            'Or run this again with the -z parameter with a collection_url to download urls first.')
        program_exit()

    # Create empty done.text if it does not exist
    if not Path(args.done_file).is_file():
        Path(args.done_file).touch()


    loader = FDownloader(
        urls_file=args.file_urls,
        done_file=args.done_file,
        cookies_file=args.cookies_file,
        root_manga_dir=args.output_dir,
        login=args.login,
        password=args.password,
        timeout=args.timeout,
        wait=args.wait,
        _max=args.max,
        debug_use_stored_html=args.debug_use_stored_html
    )



    if not args.debug_use_stored_html:
        if not Path(args.cookies_file).is_file():
            print(f'Cookies file({args.cookies_file}) are not detected. Please, ' + \
                'login in next step for generate cookie for next runs.')
            loader.init_browser(headless=False)
        else:
            print(f'Using cookies file: {args.cookies_file}')
            loader.init_browser(headless=False)

    if args.collection_url:
        loader.load_urls_from_collection(args.collection_url)
    else:
        loader.load_all()

if __name__ == '__main__':
    main()

*/


/*
Downloader.py:
import os
import pickle
import re
import time
import xml.etree.ElementTree as ET
from shutil import rmtree
from time import sleep

from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.common.exceptions import TimeoutException, JavascriptException

from bs4 import BeautifulSoup as bs
from tqdm import tqdm


BASE_URL = 'https://www.fakku.net'
LOGIN_URL = f'{BASE_URL}/login/'
# Initial display settings for headless browser. Any manga in this
# resolution will be opened correctly and with the best quality.
MAX_DISPLAY_SETTINGS = [1440, 2560]
# Path to headless driver
EXEC_PATH = 'chromedriver.exe'
# File with manga urls
URLS_FILE = 'urls.txt'
# File with completed urls
DONE_FILE = 'done.txt'
# File with prepared cookies
COOKIES_FILE = 'cookies.pickle'
# Root directory for manga downloader
ROOT_MANGA_DIR = 'manga'
# Timeout to page loading in seconds
TIMEOUT = 5
# Wait between page loading in seconds
WAIT = 2
# Max manga to download in one session (-1 == no limit)
MAX = None
# Flag to assist with debugging data-scraping codesets. Program must be run once without it to grab HTML to store in a file.
DEBUG_USE_STORED_HTML = False
# User agent for web browser
USER_AGENT = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_2) AppleWebKit/601.3.9 (KHTML, like Gecko) Version/9.0.2 Safari/601.3.9'



def program_exit():
    print('Program exit.')
    exit()


class FDownloader():
    """
    Class which allows download manga.
    The main idea of download - using headless browser and just saving
    screenshot from that. Because canvas in fakku.net is protected
    from download via simple .toDataURL js function etc.
    """
    def __init__(self,
            urls_file=URLS_FILE,
            done_file=DONE_FILE,
            cookies_file=COOKIES_FILE,
            root_manga_dir=ROOT_MANGA_DIR,
            driver_path=EXEC_PATH,
            default_display=MAX_DISPLAY_SETTINGS,
            timeout=TIMEOUT,
            wait=WAIT,
            login=None,
            password=None,
            _max=MAX,
            debug_use_stored_html=DEBUG_USE_STORED_HTML
        ):
        """
        param: urls_file -- string name of .txt file with urls
            Contains list of manga urls, that's to be downloaded
        param: done_file -- string name of .txt file with urls
            Contains list of manga urls that have successfully been downloaded
        param: cookies_file -- string name of .picle file with cookies
            Contains bynary data with cookies
        param: driver_path -- string
            Path to the headless driver
        param: default_display -- list of two int (width, height)
            Initial display settings. After loading the page, they will be changed
        param: timeout -- float
            Timeout upon waiting for first page to load
            If <5 may be poor quality.
        param: wait -- float
            Wait in seconds beetween pages downloading.
            If <1 may be poor quality.
        param: login -- string
            Login or email for authentication
        param: password -- string
            Password for authentication
        """
        self.urls_file = urls_file
        self.urls = self.__get_urls_list(urls_file, done_file)
        self.done_file = done_file
        self.cookies_file = cookies_file
        self.root_manga_dir = root_manga_dir
        self.driver_path = driver_path
        self.browser = None
        self.default_display = default_display
        self.timeout = timeout
        self.wait = wait
        self.login = login
        self.password = password
        self.max = _max
        self.debug_use_stored_html = debug_use_stored_html

    def init_browser(self, headless=False):
        """
        Initializing browser and authenticate if necessary
        Lots of obfuscation via: https://intoli.com/blog/making-chrome-headless-undetectable/
        ---------------------
        param: headless -- bool
            If True: launch browser in headless mode(for download manga)
            If False: launch usually browser with GUI(for first authenticate)
        """
        options = webdriver.ChromeOptions()
        if headless:
            options.add_argument('headless')
        options.add_argument(f'user-agent={USER_AGENT}')

        self.browser = webdriver.Chrome(
            executable_path=self.driver_path,
            chrome_options=options,
        )

        # Note: not sure if this is actually working, or needs to be called later. Tough to verify.
        customJs = """
        // overwrite the `languages` property to use a custom getter
        Object.defineProperty(navigator, 'languages', {
          get: function() {
            return ['en-US', 'en'];
          },
        });

        // overwrite the `plugins` property to use a custom getter
        Object.defineProperty(navigator, 'plugins', {
          get: function() {
            // this just needs to have `length > 0`, but we could mock the plugins too
            return [1, 2, 3, 4, 5];
          },
        });

        // Spoof renderer checks
        const getParameter = WebGLRenderingContext.getParameter;
        WebGLRenderingContext.prototype.getParameter = function(parameter) {
          // UNMASKED_VENDOR_WEBGL
          if (parameter === 37445) {
            return 'Intel Open Source Technology Center';
          }
          // UNMASKED_RENDERER_WEBGL
          if (parameter === 37446) {
            return 'Mesa DRI Intel(R) Ivybridge Mobile ';
          }

          return getParameter(parameter);
        };
        """

        self.browser.execute_script(customJs)

        if not headless:
            self.__auth()
        self.__set_cookies()
        self.browser.set_window_size(*self.default_display)

    def __set_cookies(self):
        self.browser.get(LOGIN_URL)
        with open(self.cookies_file, 'rb') as f:
            cookies = pickle.load(f)
            for cookie in cookies:
                if 'expiry' in cookie:
                    cookie['expiry'] = int(cookie['expiry'])
                    self.browser.add_cookie(cookie)

    def __init_headless_browser(self):
        """
        Recreating browser in headless mode(without GUI)
        """
        print(f'Using headless browser.')
        options = Options()
        options.headless = True
        self.browser = webdriver.Chrome(
            executable_path=self.driver_path,
            chrome_options=options)

    def __auth(self):
        """
        Authentication in browser with GUI for saving cookies in first time
        """
        try:
            self.browser.get(LOGIN_URL)
            print(f'Using login: {self.login}')
            if not self.login is None:
                print(f'Looking for username login field...')
                self.browser.find_element_by_id('username').send_keys(self.login)
                print(f'Login name applied.')
            if not self.password is None:
                print(f'Looking for password field...')
                self.browser.find_element_by_id('649c4f9a64').send_keys(self.password)
                print(f'Password applied.')
            self.browser.find_element_by_class_name('js-submit-login').click()
            print(f'Credentials submitted.')
        except Exception as ex:
            print(ex)

        ready = input("Press Enter key to continue after you login...\n\n")
        with open(self.cookies_file, 'wb') as f:
            pickle.dump(self.browser.get_cookies(), f)

        self.browser.close()
        # Recreating browser in headless mode for next manga downloading
        self.__init_headless_browser()

    def load_all(self):
        """
        Just main function which opening each page and save it in .png
        """
        if not self.debug_use_stored_html:
            self.browser.set_window_size(*self.default_display)
        if not os.path.exists(self.root_manga_dir):
            os.mkdir(self.root_manga_dir)

        with open(self.done_file, 'a') as done_file_obj:
            urls_processed = 0
            for url in self.urls:
                manga_name = url.split('/')[-1]
                manga_folder = os.sep.join([self.root_manga_dir, manga_name])
                if not os.path.exists(manga_folder):
                   os.mkdir(manga_folder)

                sPage_Source = ""
                if not self.debug_use_stored_html:
                    print(f'\nGetting url: {url}\n')
                    self.browser.get(url)
                    self.waiting_loading_page(is_reader_page=False)
                    sPage_Source = self.browser.page_source
                    fHTMLFile = open(os.sep.join([manga_folder, f'ComicSource.html']), "w", encoding="utf-8")
                    fHTMLFile.write(sPage_Source)
                    fHTMLFile.close()
                else:
                    fHTMLFile = open(os.sep.join([manga_folder, f'ComicSource.html']), "r")
                    sPage_Source = fHTMLFile.read()
                    fHTMLFile.close()

                print(f'Getting page count...')
                page_count = self.__get_page_count(sPage_Source)
                print(f' Page count: {page_count}\n')

                print(f'Getting comic title from html...')
                comic_title = self.__get_title(sPage_Source)
                print(f' Comic title: {comic_title}\n')

                print(f'Getting comic artist from html...')
                comic_artist = self.__get_comic_attr(sPage_Source, "Artist")
                print(f' Comic artist: {comic_artist}\n')

                print(f'Getting comic parody from html...')
                comic_parody = self.__get_comic_attr(sPage_Source, "Parody")
                print(f' Comic parody: {comic_parody}\n')

                print(f'Getting comic tags...')
                comic_tags = self.__get_tags(sPage_Source)
                print(f' Tags: {comic_tags}\n')

                """Create an xml file containing the comic details"""
                xmlData = ET.Element('ComicData')
                dataItemComicTitle = ET.SubElement(xmlData, 'ComicTitle')
                dataItemAddress = ET.SubElement(xmlData, 'URL')
                dataItemPageCount = ET.SubElement(xmlData, 'PageCount')
                dataItemArtist = ET.SubElement(xmlData, 'Artist')
                dataItemParody = ET.SubElement(xmlData, 'Parody')
                dataItemTags = ET.SubElement(xmlData, 'Tags')

                dataItemComicTitle.text = comic_title
                dataItemAddress.text = url
                dataItemPageCount.text = str(page_count)
                dataItemArtist.text = comic_artist
                dataItemParody.text = comic_parody

                for comic_tag in comic_tags:
                    dataItemTag = ET.SubElement(dataItemTags, 'Tag')
                    dataItemTag.text = comic_tag

                sXMLData = ET.tostring(xmlData)
                fXMLFile = open(os.sep.join([manga_folder, f'ComicData.xml']), "wb")
                fXMLFile.write(sXMLData)
                fXMLFile.close()


                print(f'Downloading "{manga_name}" manga.')
                delay_before_fetching = True # When fetching the first page, multiple pages load and the reader slows down

                for page_num in tqdm(range(1, page_count + 1)):
                    sPageNumString = str(page_num).zfill(4)
                    destination_file = os.sep.join([manga_folder, f'{sPageNumString}.png'])
                    if os.path.isfile(destination_file):
                        delay_before_fetching = True #When skipping files, the reader will load multiple pages and slow down again
                        continue
                    self.browser.get(f'{url}/read/page/{page_num}')
                    self.waiting_loading_page(is_reader_page=True, should_add_delay=delay_before_fetching)
                    delay_before_fetching = False

                    # Count of leyers may be 2 or 3 therefore we get different target layer
                    n = self.browser.execute_script("return document.getElementsByClassName('layer').length")
                    try:
                        # Resizing window size for exactly manga page size
                        width = self.browser.execute_script(f"return document.getElementsByTagName('canvas')[{n-2}].width")
                        height = self.browser.execute_script(f"return document.getElementsByTagName('canvas')[{n-2}].height")
                        self.browser.set_window_size(width, height)
                    except JavascriptException:
                        print('\nSome error with JS. Page source are note ready. You can try increase argument -t')

                    # Delete all UI and save page
                    self.browser.execute_script(f"document.getElementsByClassName('layer')[{n-1}].remove()")
                    self.browser.save_screenshot(destination_file)
                print('>> manga done!')
                if not self.debug_use_stored_html: done_file_obj.write(f'{url}\n')
                urls_processed += 1
                if self.max is not None and urls_processed >= self.max:
                    break

    def load_urls_from_collection(self, collection_url):
        """
        Function which records the manga URLs inside a collection
        """
        self.browser.get(collection_url)
        self.waiting_loading_page(is_reader_page=False)
        page_count = self.__get_page_count_in_collection(self.browser.page_source)
        with open(self.urls_file, 'a') as f:
            for page_num in tqdm(range(1, page_count + 1)):
                if page_num != 1: #Fencepost problem, the first page of a collection is already loaded
                    self.browser.get(f'{collection_url}/page/{page_num}')
                    self.waiting_loading_page(is_reader_page=False)
                soup = bs(self.browser.page_source, 'html.parser')
                for div in soup.find_all('div', attrs={'class': 'book-title'}):
                    f.write(f"{BASE_URL}{div.find('a')['href']}\n")

    def __get_page_count(self, page_source):
        """
        Get count of manga pages from html code
        ----------------------------
        param: page_source -- string
            String that contains html code
        return: int
            Number of manga pages
        """
        debug = self.debug_use_stored_html
        soup = bs(page_source, 'html.parser')
        page_count = None
        if not page_count:
            try:
                #divs = soup.find_all('div', class_='table-cell w-full align-top text-left space-y-2 link:text-blue-700 dark:link:text-white')
                #for x in divs:
                #    sDivText = x.text.strip()
                #    if sDivText != "":
                #        print(sDivText)
                #        if "pages" in sDivText:
                #            page_count = int(sDivText.split(' ')[0])
                grid_tags = soup.find_all('div', class_='table text-sm w-full')
                if debug: print("Grid_tags has size: " + str(len(grid_tags)))
                page_count_found = False
                for i in range(len(grid_tags)): #grid_tag in grid_tags:
                    if debug: print("Item " + str(i) + ":")
                    if debug: print(grid_tags[i])
                    if debug: print("Content count: " + str(len(grid_tags[i].contents)))
                    for j in range(len(grid_tags[i].contents)):
                        if debug: print("Content " + str(j) + "(length " + str(len(grid_tags[i].contents[j])) + "):")
                        if debug: print(grid_tags[i].contents[j])
                        if grid_tags[i].contents[j].string == "Pages":
                            if debug: print("FOUND PAGES!")
                            page_str = grid_tags[i].contents[j+2].string
                            if debug: print("Pages string: " + page_str)
                            page_count = int(page_str.split(' ')[0])
                            if type(page_count) is int:
                                if debug: print("Page count: " + str(page_count))
                                page_count_found = True
                                break
                    if debug: print("")
                    if page_count_found:
                        break

            except Exception as ex:
                print(ex)
        return page_count


    def __get_title(self, page_source):
        """
        Get title of comic from html code
        ----------------------------
        param: page_source -- string
            String that contains html code
        return: string
            Title of comic
        """
        debug = self.debug_use_stored_html
        soup = bs(page_source, 'html.parser')
        title = None
        if not title:
            try:
                #title = soup.find(attrs={'class': 'content-name'}).text
                #title = title.strip()

                title = soup.find('h1')
                if debug: print("Title: " + title.string)
                title = title.string

            except Exception as ex:
                print(ex)
        return title

    def __get_comic_attr(self, page_source, attr):
        """
        Get attribute of comic from html code
        ----------------------------
        param: page_source -- string
            String that contains html code
        return: string
            Requested attribute of comic
        """
        debug = self.debug_use_stored_html
        soup = bs(page_source, 'html.parser')
        attr_result = None
        attr_found = False
        if not attr_result:
            try:
                #divs = soup.find_all('div', attrs={'class': 'row'})
                #attr_result = next(x for x in divs if x(text=attr)).find('div', attrs={'class': 'row-right'}).text
                #attr_result = attr_result.strip()


                grid_tags = soup.find_all('div', class_='table text-sm w-full')
                if debug: print("Grid_tags has size: " + str(len(grid_tags)))
                page_count_found = False
                for i in range(len(grid_tags)): #grid_tag in grid_tags:
                    if debug: print("Item " + str(i) + ":")
                    if debug: print(grid_tags[i])
                    if debug: print("Content count: " + str(len(grid_tags[i].contents)))
                    for j in range(len(grid_tags[i].contents)):
                        if debug: print("Content " + str(j) + "(length " + str(len(grid_tags[i].contents[j])) + "):")
                        if debug: print(grid_tags[i].contents[j])
                        if grid_tags[i].contents[j].string == attr:
                            if debug: print("FOUND ATTRIBUTE: " + attr)
                            attr_result = grid_tags[i].contents[j+2].string
                            attr_result = attr_result.strip()
                            if debug: print("Attribute value: " + attr_result)
                            attr_found = True
                            break
                    if debug: print("")
                    if attr_found:
                        break

            except Exception as ex:
                print(ex)
        return attr_result

    def __get_tags(self, page_source):
        """
        Get tags for comic from html code
        ----------------------------
        param: page_source -- string
            String that contains html code
        return: string[]
            Comic tags
        """
        debug = self.debug_use_stored_html
        soup = bs(page_source, 'html.parser')
        tags = []
        try:
            #Find all links. Tags in FAKKU are contained in links.
            afind = soup.find_all('a')
            #Filter-down to links that have a particular parent class:
            #for ana in afind:
            #    #.parent.attrs returns a dictionary object.
            #    dictAttrs = ana.parent.attrs
            #    if isinstance(ana.parent.attrs, dict):
            #        arrayAttrs = dictAttrs.get('class')
            #        if isinstance(arrayAttrs, list):
            #            if len(arrayAttrs) == 2:
            #                if arrayAttrs[0]=='row-right' and arrayAttrs[1]=='tags':
            #                    #Get the text
            #                    sTagText = ana.get_text()
            #                    sTagText = sTagText.strip()
            #                    if not(sTagText == '+' or sTagText == 'unlimited') :
            #                        #FAKKU includes the '+' symbol to recommend a tag.
            #                        #'unlimited' is a subscription status tag classification for a comic.
            #                        tags.append(sTagText)
            if debug: print("====================TAG SEARCH=================")
            for ana in afind:
                #.parent.attrs returns a dictionary object.
                #print(ana.get('href'))
                link = ana.get('href')
                #print("Type: " + str(type(link)))
                if link is not None:
                    if ana.get('href').startswith("/tags/"):
                        if debug: print("Found a tag:" + ana.get('href'))
                        #Get the text
                        sTagText = ana.get_text()
                        sTagText = sTagText.strip()
                        if not(sTagText == '+' or sTagText == 'Unlimited') :
                            #FAKKU includes the '+' symbol to recommend a tag.
                            #'unlimited' is a subscription status tag classification for a comic.
                            tags.append(sTagText)

        except Exception as ex:
            print(ex)
        return tags


    def __get_page_count_in_collection(self, page_source):
        """
        Get count of collection pages from html code
        ----------------------------
        param: page_source -- string
            String that contains html code
        return: int
            Number of collection pages
        """
        soup = bs(page_source, 'html.parser')
        page_count = None
        if not page_count:
            try:
                pagination_text = soup.find('div', attrs={'class': 'pagination-meta'}).text
                page_count = int(re.search(r"Page\s+\d+\s+of\s+(\d+)", pagination_text).group(1))
            except Exception as ex:
                print(ex)
        return page_count


    def __get_urls_list(self, urls_file, done_file):
        """
        Get list of urls from .txt file
        --------------------------
        param: urls_file -- string
            Name or path of .txt file with manga urls
        param: done_file -- string
            Name or path of .txt file with successfully downloaded manga urls
        return: urls -- list
            List of urls from urls_file
        """
        done = []
        with open(done_file, 'r') as donef:
            for line in donef:
                done.append(line.replace('\n',''))

        urls = []
        with open(urls_file, 'r') as f:
            for line in f:
                clean_line = line.replace('\n','')
                if clean_line not in done:
                    urls.append(clean_line)
        return urls

    def waiting_loading_page(self, is_reader_page=False, should_add_delay=False):
        """
        Awaiting while page will load
        ---------------------------
        param: is_non_reader_page -- bool
            False -- awaiting of main manga page
            True -- awaiting of others manga pages
        param: should_add_delay -- bool
            False -- the page num != 1
            True -- this is the first page, we need to wait longer to get good quality
        """
        if not is_reader_page:
            sleep(self.wait)
            elem_xpath = "//link[@type='image/x-icon']"
        elif should_add_delay:
            sleep(self.wait * 3)
            elem_xpath = "//div[@data-name='PageView']"
        else:
            sleep(self.wait)
            elem_xpath = "//div[@data-name='PageView']"
        try:
            element = EC.presence_of_element_located((By.XPATH, elem_xpath))
            WebDriverWait(self.browser, self.timeout).until(element)
        except TimeoutException:
            print('\nError: timed out waiting for page to load. + \
                You can try increase param -t for more delaying.')
            program_exit()



*/


