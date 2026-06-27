import sys


class MyLog:
    def __init__(self):
        pass

    def LINE(self):
        return sys._getframe(1).f_lineno